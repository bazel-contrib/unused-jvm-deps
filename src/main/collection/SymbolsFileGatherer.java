package com.stripe.build.dependencyanalyzer.collection;

import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelTarget;
import com.stripe.build.dependencyanalyzer.plugin.SymbolCollectionResult;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SymbolsFileGatherer {

  private final Path bazelWorkspace;
  private final Set<SymbolCollectionResult> symbolsResults = new HashSet<>();

  /**
   * Gathers all symbols.json files from the bazel-bin for a set of Bazel targets and returns the
   * parsed results.
   *
   * @param bazelWorkspace Bazel workspace root directory (should contain a WORKSPACE file)
   * @param targets the set of Bazel targets for which to collect symbols files
   * @return result object containing parsed {@link SymbolCollectionResult} objects
   * @throws IOException if error occurs reading a symbols file
   */
  public static SymbolsFileGatherResult getSymbolsForTargets(
      Path bazelWorkspace, Collection<BazelTarget> targets) throws IOException {
    return new SymbolsFileGatherer(bazelWorkspace).gatherSymbolsForTargets(targets);
  }

  private SymbolsFileGatherer(Path bazelWorkspace) {
    this.bazelWorkspace = bazelWorkspace;
  }

  private SymbolsFileGatherResult gatherSymbolsForTargets(Collection<BazelTarget> targets)
      throws IOException {
    Instant startTime = Clock.systemUTC().instant();
    Set<String> targetLabelSet =
        targets.stream().map(BazelTarget::getTargetLabel).collect(Collectors.toSet());

    try (Stream<Path> paths =
        Files.walk(bazelWorkspace.resolve("bazel-bin"), FileVisitOption.FOLLOW_LINKS)) {
      Set<Path> allSymbolsFiles =
          paths.filter(p -> p.toString().endsWith("symbols.json")).collect(Collectors.toSet());
      for (Path symbolsFile : allSymbolsFiles) {
        try (Reader reader = Files.newBufferedReader(symbolsFile)) {
          var parser = new JsonStreamParser(reader);
          var doc = parser.next();
          Iterable<JsonElement> array;

          if (doc.isJsonArray()) {
            array = doc.getAsJsonArray();
          } else {
            array = List.of(doc);
          }
          for (JsonElement jsonElement : array) {
            if (jsonElement.isJsonNull()) {
              continue;
            }
            SymbolCollectionResult result =
                SymbolCollectionResult.fromJsonObject(jsonElement.getAsJsonObject());
            if (targetLabelSet.contains(result.getBazelTargetLabel())) {
              symbolsResults.add(result);
            }
          }
        } catch (Exception e) {
          System.out.println("WARNING: Error reading symbols file: " + symbolsFile + ": " + e);
          e.printStackTrace();
        }
      }
    }
    Instant endTime = Clock.systemUTC().instant();
    return SymbolsFileGatherResult.create(symbolsResults, Duration.between(startTime, endTime));
  }
}
