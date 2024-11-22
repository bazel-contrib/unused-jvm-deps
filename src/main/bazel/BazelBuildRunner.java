package com.stripe.build.dependencyanalyzer.bazel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;

public class BazelBuildRunner {

  private final Path bazelWorkspace;

  private BazelBuildRunner(Path bazelWorkspace) {
    this.bazelWorkspace = bazelWorkspace;
  }

  public static BazelBuildResult runBuild(
      String target, List<String> bazelFlags, Path bazelWorkspace, boolean streamOutput)
      throws IOException, InterruptedException {
    return new BazelBuildRunner(bazelWorkspace).run(target, bazelFlags, streamOutput);
  }

  private BazelBuildResult run(String target, List<String> bazelFlags, boolean streamOutput)
      throws IOException, InterruptedException {
    Instant startTime = Clock.systemUTC().instant();
    List<String> args = new ArrayList<>();
    args.add("bazel");
    args.add("build");
    args.add(target);
    args.addAll(bazelFlags);
    System.out.println("Running " + String.join(" ", args));
    var buildProcBuilder =
        new ProcessBuilder().directory(new File(bazelWorkspace.toString())).command(args);

    String bazelOutput;
    Process buildProc;
    if (streamOutput) {
      buildProc = buildProcBuilder.inheritIO().start();
      bazelOutput = "Streamed above.";
    } else {
      buildProc = buildProcBuilder.start();
      bazelOutput = IOUtils.toString(buildProc.getErrorStream(), StandardCharsets.UTF_8).trim();
    }
    int exitCode = buildProc.waitFor();
    Instant endTime = Clock.systemUTC().instant();
    return BazelBuildResult.create(bazelOutput, Duration.between(startTime, endTime), exitCode);
  }
}
