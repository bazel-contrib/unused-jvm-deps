package com.stripe.build.dependencyanalyzer.bazel;

import com.google.auto.value.AutoValue;
import java.time.Duration;

@AutoValue
public abstract class BazelBuildResult {
  public abstract String getBazelOutput();

  public abstract Duration getTimeElapsed();

  public abstract int exitCode();

  public static BazelBuildResult create(String bazelOutput, Duration timeElapsed, int exitCode) {
    return new AutoValue_BazelBuildResult(bazelOutput, timeElapsed, exitCode);
  }

  @Override
  public final String toString() {
    return "Bazel build finished with code " + exitCode() + "\n"
        + String.format("Time elapsed: %d seconds\n", getTimeElapsed().getSeconds());
  }
}
