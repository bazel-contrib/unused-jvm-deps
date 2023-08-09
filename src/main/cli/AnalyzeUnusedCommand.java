package com.stripe.build.dependencyanalyzer.cli;

import com.stripe.build.dependencyanalyzer.analysis.BazelTargetFilter;
import com.stripe.build.dependencyanalyzer.analysis.ThirdPartySymbolsIndex;
import com.stripe.build.dependencyanalyzer.analysis.UnusedDepsAnalysisResult;
import com.stripe.build.dependencyanalyzer.analysis.UnusedDepsAnalyzer;
import com.stripe.build.dependencyanalyzer.database.Database;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "unused",
    description =
        "Performs a Bazel unused dependency analysis using the dependency graph stored in the input"
            + " database")
public class AnalyzeUnusedCommand implements Runnable {

  @Nullable
  @Parameters(paramLabel = "<databaseFile>", description = "Path to database file to read")
  private String databaseFile;

  @Nullable
  @Option(
      names = {"-t", "--third-party-index"},
      description = "CSV for the third-party-index",
      defaultValue = "")
  private String thirdPartyIndexCsv;

  @Nullable
  @Option(
      names = {"--third-party-index-delimiter"},
      description = "Delimiter for CSV file of third-party-index",
      defaultValue = ",",
      type = Character.class)
  private Character thirdPartyIndexCsvDelimiter;

  @Nullable
  @Option(
      names = {"-x", "--excluded-dep"},
      description = "Bazel deps that should be excluded from removal suggestions")
  private Set<String> excludedDependencies;

  @Nullable
  @Option(
      names = {"-f", "--filter"},
      description =
          "Optionally add a filter to only output commands to fix BUILD files in a certain"
              + " subdirectory",
      defaultValue = "")
  private String filter;

  @Nullable
  @Option(
      names = {"-o", "--output"},
      description = "Optionally provide an output file to which to write buildozer commands")
  private String outputFile;

  @Option(
      names = {"-d", "--debug"},
      description = "Print full error stacktrace")
  private boolean debug;

  @Override
  public void run() {
    try {
      if (databaseFile == null || thirdPartyIndexCsvDelimiter == null) {
        throw new IllegalArgumentException(
            "Passed null argument, which should have been handled by Picocli.");
      }
      Path indexPath = null;
      if (thirdPartyIndexCsv != null && !thirdPartyIndexCsv.isEmpty()) {
        indexPath = Path.of(thirdPartyIndexCsv);
      }
      if (excludedDependencies == null) {
        excludedDependencies = Set.of();
      }

      Database database = Database.open(Path.of(databaseFile));
      UnusedDepsAnalysisResult analysisResult =
          UnusedDepsAnalyzer.analyze(
              database,
              new ThirdPartySymbolsIndex(indexPath, thirdPartyIndexCsvDelimiter),
              new BazelTargetFilter(StringUtils.defaultIfEmpty(filter, "")),
              excludedDependencies);
      outputResult(analysisResult);

    } catch (SQLException | IOException e) {
      System.out.println("An error occurred during analysis");
      if (debug) {
        System.out.println("Stack Trace:");
        System.out.println(ExceptionUtils.getStackTrace(e));
      } else {
        System.out.println("Message: " + ExceptionUtils.getMessage(e));
        System.out.println("For full stack trace, use the --debug option!");
      }
    }
  }

  /**
   * Outputs the analysis result, writing the buildozer commands either to stdout or the file
   * provided by the --output flag.
   *
   * @param result the analysis result
   * @throws IOException if an error occurs opening or writing to the user provided file
   */
  private void outputResult(UnusedDepsAnalysisResult result) throws IOException {
    System.out.println(result);
    if (result.getUnusedEdges().isEmpty()) {
      System.out.println("No unused dependencies were found.");
      if (!result.getIgnoredEdges().isEmpty()) {
        System.out.println(
            "Ignored dependencies: \n- "
                + result.getIgnoredEdges().stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining("\n- ")));
      }
      return;
    }
    Writer writer;
    if (StringUtils.isBlank(outputFile)) {
      System.out.println("Buildozer commands to remove unused dependencies:");
      writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
    } else {
      writer = Files.newBufferedWriter(Path.of(outputFile));
      System.out.println(
          "Wrote buildozer commands to remove unused dependencies to: " + outputFile);
    }
    for (String buildozerCommand : result.getBuildozerCommandsToRemoveUnusedDeps()) {
      writer.write(buildozerCommand + "\n");
      writer.flush();
    }
  }
}
