package com.stripe.build.dependencyanalyzer.bazel;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * A label that uniquely identifies a rule, e.g:
 *
 * <p>{@code //src/main/com/stripe/mergeq/db:util}
 */
@AutoValue
public abstract class BazelRuleLabel {
  public abstract String value();

  public static BazelRuleLabel of(String value) {
    return new AutoValue_BazelRuleLabel(value);
  }

  public static Optional<BazelRuleLabel> fromInputLabel(Directory parentDir, String value) {
    if (value.startsWith(":")) {
      // We have a relative path to a label in the current directory, construct the full rule label
      // from its value:
      return Optional.of(BazelRuleLabel.of("//" + parentDir.value() + value));
    }

    if (BazelFileLabel.of(value).isPresent()) {
      return Optional.empty();
    }

    return Optional.of(BazelRuleLabel.of(value));
  }
}
