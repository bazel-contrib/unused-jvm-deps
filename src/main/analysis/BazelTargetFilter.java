package com.stripe.build.dependencyanalyzer.analysis;

import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelTarget;

public class BazelTargetFilter {
  private final String prefix;

  public BazelTargetFilter(String prefix) {
    if (!prefix.startsWith("//")) {
      throw new IllegalArgumentException("prefix must start with // but doesn't: " + prefix);
    }
    this.prefix = prefix;
  }

  public boolean matches(BazelTarget target) {
    return target.getTargetLabel().startsWith(prefix);
  }

  @Override
  public String toString() {
    return "prefix=" + prefix;
  }
}
