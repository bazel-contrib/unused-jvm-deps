package com.stripe.build.dependencyanalyzer.collection;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.stripe.build.dependencyanalyzer.plugin.SymbolCollectionResult;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
public abstract class SymbolsFileGatherResult {
  public abstract ImmutableSet<SymbolCollectionResult> getSymbolResults();

  public abstract Duration getTimeElapsed();

  public static SymbolsFileGatherResult create(
      Set<SymbolCollectionResult> symbolResults, Duration timeElapsed) {
    return new AutoValue_SymbolsFileGatherResult(ImmutableSet.copyOf(symbolResults), timeElapsed);
  }

  public Set<String> getAllSymbols() {
    return getSymbolResults().stream()
        .map(SymbolCollectionResult::getAllSymbols)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public final String toString() {
    var sb =
        new StringBuilder(String.format("Gathered %d symbols files\n", getSymbolResults().size()));
    sb.append(String.format("Time elapsed: %d seconds\n", getTimeElapsed().getSeconds()));
    return sb.toString();
  }
}
