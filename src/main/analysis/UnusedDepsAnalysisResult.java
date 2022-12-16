package com.stripe.build.dependencyanalyzer.analysis;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

@AutoValue
public abstract class UnusedDepsAnalysisResult {

  public abstract ImmutableSet<RichBazelEdge> getIgnoredEdges();

  public abstract ImmutableSet<RichBazelEdge> getUsedEdges();

  public abstract ImmutableSet<RichBazelEdge> getUnusedEdges();

  public abstract Duration getTimeElapsed();

  public static UnusedDepsAnalysisResult create(
      Collection<RichBazelEdge> ignoredEdges,
      Collection<RichBazelEdge> usedEdges,
      Collection<RichBazelEdge> unusedEdges,
      Duration timeElapsed) {
    return new AutoValue_UnusedDepsAnalysisResult(
        ImmutableSet.copyOf(ignoredEdges),
        ImmutableSet.copyOf(usedEdges),
        ImmutableSet.copyOf(unusedEdges),
        timeElapsed);
  }

  public List<String> getBuildozerCommandsToRemoveUnusedDeps() {
    return getUnusedEdges().stream()
        .sorted(
            (RichBazelEdge edge1, RichBazelEdge edge2) ->
                StringUtils.compare(edge1.getSortableString(), edge2.getSortableString()))
        .map(RichBazelEdge::getBuildozerCommandToDeleteEdge)
        .collect(Collectors.toList());
  }

  @Override
  public final String toString() {
    String separator = "-".repeat(30) + "\n";
    int numEdgesConsidered = getUsedEdges().size() + getUnusedEdges().size();
    return String.format("Ignored dependencies: %d\n", getIgnoredEdges().size())
        + separator
        + String.format("Considered dependencies: %d\n", numEdgesConsidered)
        + String.format(
            "Used dependencies: %d (%d%%)\n",
            getUsedEdges().size(), percent(getUsedEdges().size(), numEdgesConsidered))
        + String.format(
            "Unused dependencies: %d (%d%%)\n",
            getUnusedEdges().size(), percent(getUnusedEdges().size(), numEdgesConsidered))
        + separator
        + String.format("Time elapsed: %d seconds", getTimeElapsed().getSeconds());
  }

  private static int percent(int numerator, int denominator) {
    return Math.round(100 * (float) numerator / denominator);
  }
}
