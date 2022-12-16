package com.stripe.build.dependencyanalyzer.analysis;

import com.google.auto.value.AutoValue;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelTarget;

@AutoValue
public abstract class RichBazelEdge {
  public abstract BazelTarget getFromTarget();

  public abstract BazelTarget getToTarget();

  public static RichBazelEdge create(BazelTarget fromTarget, BazelTarget toTarget) {
    return new AutoValue_RichBazelEdge(fromTarget, toTarget);
  }

  /**
   * Returns the buildozer command to remove the target labeled by toTarget from the BUILD file of
   * the target labeled by fromTarget, thus removing this edge from the graph.
   *
   * @return String buildozer command
   */
  public String getBuildozerCommandToDeleteEdge() {
    return String.format(
        "buildozer 'remove deps %s' %s",
        getToTarget().getTargetLabel(), getFromTarget().getTargetLabel());
  }

  /** Gets a string to use in comparator to sort buildozer commands to be the most readable. */
  public String getSortableString() {
    return getFromTarget().getTargetLabel() + " " + getToTarget().getTargetLabel();
  }

  @Override
  public String toString() {
    return getFromTarget().getTargetLabel() + " → " + getToTarget().getTargetLabel();
  }
}
