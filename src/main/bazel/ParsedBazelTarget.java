package com.stripe.build.dependencyanalyzer.bazel;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;

@AutoValue
public abstract class ParsedBazelTarget {
  public abstract BazelRuleLabel getLabel();

  public abstract ImmutableSet<BazelRuleLabel> getDeps();

  public abstract ImmutableSet<BazelRuleLabel> getExports();

  public static ParsedBazelTarget create(
      BazelRuleLabel label, Collection<BazelRuleLabel> deps, Collection<BazelRuleLabel> exports) {
    return new AutoValue_ParsedBazelTarget(
        label, ImmutableSet.copyOf(deps), ImmutableSet.copyOf(exports));
  }
}
