package com.stripe.build.dependencyanalyzer.cli;

import com.stripe.build.dependencyanalyzer.bazel.BazelBuildResult;
import com.stripe.build.dependencyanalyzer.bazel.BazelBuildRunner;
import com.stripe.build.dependencyanalyzer.bazel.TargetDependencyGraphLoadResult;
import com.stripe.build.dependencyanalyzer.bazel.TargetDependencyGraphLoader;
import com.stripe.build.dependencyanalyzer.collection.SymbolsFileGatherResult;
import com.stripe.build.dependencyanalyzer.collection.SymbolsFileGatherer;
import com.stripe.build.dependencyanalyzer.collection.SymbolsPersistResult;
import com.stripe.build.dependencyanalyzer.collection.SymbolsPersister;
import com.stripe.build.dependencyanalyzer.collection.TargetDependencyGraphPersistResult;
import com.stripe.build.dependencyanalyzer.collection.TargetDependencyGraphPersister;
import com.stripe.build.dependencyanalyzer.database.Database;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "collect", description = "Constructs a local database from the Bazel build graph")
public class CollectCommand implements Callable<Integer> {

  @Nullable
  @Parameters(paramLabel = "<bazelTargetPattern>", description = "Root bazel target pattern")
  private String bazelTarget;

  @Nullable
  @Parameters(paramLabel = "<bazelWorkspace>", description = "Bazel workspace root directory")
  private String bazelWorkspace;

  @Nullable
  @Parameters(paramLabel = "<outputFile>", description = "Path to database file to write")
  private String outputFile;

  @Option(
      names = {"--skip_compilation"},
      description = "Skip the compilation step that generates the symbols files")
  private boolean skipCompilation;

  @Option(
      names = {"--ignore_cache"},
      description = "Ignore the action and disk caches")
  private boolean ignoreCache;

  @Option(
      names = {"-d", "--debug"},
      description = "Print full error stacktrace")
  private boolean debug;

  @Override
  public Integer call() {
    try {
      // these null checks are already handled by picocli but are needed to satisfy NullAway
      if (bazelTarget == null || bazelWorkspace == null || outputFile == null) {
        throw new IllegalArgumentException(
            "Passed null argument, which should have been handled by Picocli.");
      }
      Database database = Database.createNew(Path.of(outputFile));

      System.out.println("Querying Bazel to get Bazel dependency graph for target...");
      TargetDependencyGraphLoadResult loadResult =
          TargetDependencyGraphLoader.load(bazelTarget, Path.of(bazelWorkspace));
      System.out.println(loadResult.getDisplay(debug));

      if (loadResult.getParsedTargets().isEmpty()) {
        System.out.println("Query produced no results; exiting");
        System.out.println();
        System.out.println("Raw Bazel output:");
        System.out.println();
        System.out.println(loadResult.getBazelErrors());
        return 1;
      }

      System.out.println("Persisting Bazel query results to database...");
      TargetDependencyGraphPersistResult persistResult =
          TargetDependencyGraphPersister.persist(loadResult.getParsedTargets(), database);
      System.out.println(persistResult);

      if (skipCompilation) {
        System.out.println("Skipping compiler plugin build step");
      } else {
        System.out.println("Using compiler plugin to gather Java source symbols...");
        List<String> bazelFlags = new ArrayList<>();
        var tmpdir = Files.createTempDirectory("unused_deps");
        // TODO: delete on exit
        for (var file : List.of("WORKSPACE", "BUILD", "defs.bzl")) {
          Files.copy(
              CollectCommand.class
                  .getClassLoader()
                  .getResourceAsStream("resources/cli/" + file + ".override"),
              tmpdir.resolve(file));
        }
        Files.copy(
            CollectCommand.class
                .getClassLoader()
                .getResourceAsStream("main/plugin/plugin-binary_deploy.jar"),
            tmpdir.resolve("plugin-binary_deploy.jar"));

        bazelFlags.add("--override_repository=unused_deps=" + tmpdir.toAbsolutePath());
        bazelFlags.add("--aspects=@unused_deps//:defs.bzl%analyzer");
        bazelFlags.add("--output_groups=unused_deps_analysis_file");
        if (ignoreCache) {
          bazelFlags.add("--disk_cache=");
          bazelFlags.add(String.format("--action_env=\"time=%d\"", System.currentTimeMillis()));
        }
        bazelFlags.add("--announce_rc");
        bazelFlags.add("--curses=no");
        bazelFlags.add("--sandbox_debug");
        bazelFlags.add("--verbose_failures");

        BazelBuildResult buildResult =
            BazelBuildRunner.runBuild(bazelTarget, bazelFlags, Path.of(bazelWorkspace), debug);
        System.out.println(buildResult);
      }

      System.out.println("Gathering generated *-symbols.json files...");
      SymbolsFileGatherResult symbolsFileGatherResult =
          SymbolsFileGatherer.getSymbolsForTargets(
              Path.of(bazelWorkspace), database.getAllBazelTargets());
      if (symbolsFileGatherResult.getSymbolResults().isEmpty()) {
        System.out.println("Failed - did not find any symbol files in " + bazelWorkspace);
        return 1;
      }
      System.out.println(symbolsFileGatherResult);

      System.out.println("Persisting Java source symbols to database...");
      SymbolsPersistResult symbolsPersistResult =
          SymbolsPersister.persistSymbols(symbolsFileGatherResult, database);
      System.out.println(symbolsPersistResult);

    } catch (SQLException | IOException | IllegalArgumentException | InterruptedException e) {
      System.out.println(
          "An error occurred while collecting and persisting the Bazel dependency graph.");
      if (debug) {
        System.out.println("Stack Trace:");
        System.out.println(ExceptionUtils.getStackTrace(e));
      } else {
        System.out.println("Message: " + ExceptionUtils.getMessage(e));
        System.out.println("For full stack trace, use the --debug option!");
      }
    }
    return 0;
  }
}
