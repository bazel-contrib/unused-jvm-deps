package com.stripe.build.dependencyanalyzer.collection;

import com.stripe.build.dependencyanalyzer.bazel.BazelRuleLabel;
import com.stripe.build.dependencyanalyzer.bazel.ParsedBazelTarget;
import com.stripe.build.dependencyanalyzer.database.Database;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelEdge;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelExportEdge;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelTarget;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetDependencyGraphPersister {

  private final Database database;

  private TargetDependencyGraphPersister(Database database) {
    this.database = database;
  }

  /**
   * Persists in a SQLite file the targets and directed dependency edges between targets that make
   * up the Bazel dependency graph.
   *
   * @param parsedBazelTargets the Bazel dependency graph to persist
   * @param parsedBazelTargets the database in which to persist the graph
   * @return a {@link TargetDependencyGraphPersistResult} object containing information about the
   *     result
   * @throws IOException if error occurs
   */
  public static TargetDependencyGraphPersistResult persist(
      Map<BazelRuleLabel, ParsedBazelTarget> parsedBazelTargets, Database database)
      throws IOException {
    return new TargetDependencyGraphPersister(database).persist(parsedBazelTargets);
  }

  private TargetDependencyGraphPersistResult persist(
      Map<BazelRuleLabel, ParsedBazelTarget> labelToParsedTarget) throws IOException {
    Instant startTime = Clock.systemUTC().instant();

    // insert all targets
    Set<BazelRuleLabel> allTargetLabels = new HashSet<>();
    for (ParsedBazelTarget parsedTarget : labelToParsedTarget.values()) {
      allTargetLabels.add(parsedTarget.getLabel());
      allTargetLabels.addAll(parsedTarget.getDeps());
    }

    Set<BazelTarget> targetsToInsert = new HashSet<>();
    int targetId = 1;
    for (BazelRuleLabel label : allTargetLabels) {
      targetsToInsert.add(new BazelTarget(targetId++, label.value()));
    }
    database.bulkInsertBazelTargets(targetsToInsert);
    targetsToInsert.clear();

    Map<String, BazelTarget> labelToInsertedTarget =
        database.getAllBazelTargets().stream()
            .collect(Collectors.toMap(BazelTarget::getTargetLabel, target -> target));

    Set<BazelEdge> bazelEdgesToInsert = new HashSet<>();
    Set<BazelExportEdge> exportEdgesToInsert = new HashSet<>();
    for (ParsedBazelTarget fromParsedTarget : labelToParsedTarget.values()) {
      BazelTarget fromInsertedTarget =
          Objects.requireNonNull(labelToInsertedTarget.get(fromParsedTarget.getLabel().value()));
      for (BazelRuleLabel depLabel : fromParsedTarget.getDeps()) {
        BazelTarget toInsertedTarget =
            Objects.requireNonNull(labelToInsertedTarget.get(depLabel.value()));
        bazelEdgesToInsert.add(
            new BazelEdge(fromInsertedTarget.getTargetId(), toInsertedTarget.getTargetId()));
      }

      for (BazelRuleLabel exportedLabel : fromParsedTarget.getExports()) {
        BazelTarget exportedTarget =
            Objects.requireNonNull(labelToInsertedTarget.get(exportedLabel.value()));
        exportEdgesToInsert.add(
            new BazelExportEdge(fromInsertedTarget.getTargetId(), exportedTarget.getTargetId()));
      }
    }
    database.bulkInsertBazelEdges(bazelEdgesToInsert);
    database.bulkInsertExportEdges(exportEdgesToInsert);
    Instant endTime = Clock.systemUTC().instant();
    return TargetDependencyGraphPersistResult.create(
        database.getFilePath(),
        database.getAllBazelTargets().size(),
        database.getAllBazelEdges().size(),
        database.getFileSize(),
        Duration.between(startTime, endTime));
  }
}
