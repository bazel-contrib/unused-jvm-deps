package com.stripe.build.dependencyanalyzer.bazel;

import com.google.auto.value.AutoValue;
import java.time.Duration;

@AutoValue
public abstract class BazelBuildResult {
  public abstract String getBazelOutput();

  public abstract Duration getTimeElapsed();

  public static BazelBuildResult create(String bazelOutput, Duration timeElapsed) {
    return new AutoValue_BazelBuildResult(bazelOutput, timeElapsed);
  }

  @Override
  public final String toString() {
    return "Bazel build completed\n"
        + String.format("Time elapsed: %d seconds\n", getTimeElapsed().getSeconds());
  }
}
