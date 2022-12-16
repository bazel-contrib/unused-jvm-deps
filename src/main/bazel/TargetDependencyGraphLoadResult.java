package com.stripe.build.dependencyanalyzer.bazel;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.Map;

@AutoValue
public abstract class TargetDependencyGraphLoadResult {
  public abstract String getTarget();

  public abstract String getBazelQuery();

  public abstract String getBazelErrors();

  public abstract ImmutableMap<BazelRuleLabel, ParsedBazelTarget> getParsedTargets();

  public abstract Duration getTimeElapsed();

  public static TargetDependencyGraphLoadResult create(
      String bazelTarget,
      String bazelQuery,
      String bazelErrors,
      Map<BazelRuleLabel, ParsedBazelTarget> parsedTargets,
      Duration timeElapsed) {
    return new AutoValue_TargetDependencyGraphLoadResult(
        bazelTarget, bazelQuery, bazelErrors, ImmutableMap.copyOf(parsedTargets), timeElapsed);
  }

  public String getDisplay(boolean debug) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Loaded Bazel dependency graph of target: %s\n", getTarget()));
    sb.append(String.format("Bazel query: %s\n", getBazelQuery()));
    sb.append(String.format("Time elapsed: %d seconds\n", getTimeElapsed().getSeconds()));
    if (debug) {
      sb.append(String.format("Bazel errors:\n%s", getBazelErrors()));
    }
    return sb.toString();
  }

  @Override
  public final String toString() {
    return getDisplay(true);
  }
}
