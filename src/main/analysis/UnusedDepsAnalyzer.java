package com.stripe.build.dependencyanalyzer.analysis;

import com.google.common.collect.ImmutableSet;
import com.stripe.build.dependencyanalyzer.database.Database;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelEdge;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelExportEdge;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelTarget;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFile;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileBazelTarget;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileExportedSymbol;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileImportedSymbol;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.Symbol;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UnusedDepsAnalyzer {

  /**
   * The dependency analyzer compiler plugin can't examine itself and its own dependencies, so those
   * targets should not be persisted.
   */
  private static final Set<String> DEPENDENCY_ANALYZER_AND_ITS_DEPS = Set.of();

  private static final Set<String> MISC_IGNORED_DEPS = Set.of();

  private static final ImmutableSet<String> IGNORED_DEPS =
      ImmutableSet.<String>builder()
          .addAll(ThirdPartySymbolsIndex.ALL_EXCLUDED_TARGETS)
          .addAll(MISC_IGNORED_DEPS)
          .build();

  private final Database database;
  private final ThirdPartySymbolsIndex thirdPartySymbolsIndex;
  private final BazelTargetFilter targetFilter;

  private final Map<Integer, BazelTarget> targetIdToBazelTarget;
  private final Map<String, BazelTarget> targetLabelToBazelTarget;
  private final Map<Integer, JavaFile> fileIdToJavaFile;
  private final Map<Integer, Symbol> symbolIdToSymbol;
  private final Map<Integer, JavaFileExportedSymbol> symbolIdToJavaFileExportedSymbol;
  private final Map<JavaFile, Set<BazelTarget>> javaFileToBazelTargets;
  /**
   * Maps an exported target (by database id) to the set of targets that export it via the 'exports'
   * Bazel rule attribute.
   *
   * <p>Example:
   *
   * <pre>{@code
   * java_library(name = "libA", exports = ["libC"])
   * java_library(name = "libB", exports = ["libC"])
   * java_library(name = "libC")
   * results in a mapping:
   * BazelTarget("libC").targetId -> [BazelTarget("libA"), BazelTarget("libB")]
   * }</pre>
   */
  private final Map<Integer, Set<BazelTarget>> exportedTargetIdToExporterTargets;

  /**
   * Analyzes the dependency graph stored in the database and outputs the unused dependencies that
   * can be removed. Performs an unused dependency analysis on the dependency graph stored in the
   * given database, returning a list of unused dependency edges that can be used to
   * programmatically trim the dependency graph.
   *
   * @param dependencyGraphDatabase database containing the dependency graph
   * @param thirdPartySymbolsIndex an index of third-party symbols with fully qualified names
   * @param targetFilter only suggest removing entries from BUILD files in targets that satisfy the
   *     filter.
   * @return an unused dependency analysis
   */
  public static UnusedDepsAnalysisResult analyze(
      Database dependencyGraphDatabase,
      ThirdPartySymbolsIndex thirdPartySymbolsIndex,
      BazelTargetFilter targetFilter) {
    return new UnusedDepsAnalyzer(dependencyGraphDatabase, thirdPartySymbolsIndex, targetFilter)
        .performAnalysis();
  }

  private UnusedDepsAnalyzer(
      Database dependencyGraphDatabase,
      ThirdPartySymbolsIndex thirdPartySymbolsIndex,
      BazelTargetFilter targetFilter) {
    this.database = dependencyGraphDatabase;
    this.thirdPartySymbolsIndex = thirdPartySymbolsIndex;
    this.targetFilter = targetFilter;

    targetIdToBazelTarget = getAllBazelTargetsById();
    targetLabelToBazelTarget = getAllBazelTargetsByLabel();
    fileIdToJavaFile = getAllJavaFilesById();
    symbolIdToSymbol = getAllSymbolsById();
    symbolIdToJavaFileExportedSymbol = getJavaFileExportedSymbolsBySymbolId();
    javaFileToBazelTargets = getJavaFileToBazelTargets();
    exportedTargetIdToExporterTargets = getExportedTargetIdToExporterTargets();
  }

  private UnusedDepsAnalysisResult performAnalysis() {
    Instant startTime = Instant.now();

    // all Bazel dependency edges explicitly specified by BUILD file
    List<BazelEdge> allBazelDependencyEdges = database.getAllBazelEdges();
    // actual Bazel dependency edges determined from Java source file dependencies
    Set<BazelEdge> actualBazelDependencyEdges =
        getActualBazelDependencyEdges(database.getAllJavaFileImportedSymbols());

    List<RichBazelEdge> ignoredBazelDependencyEdges = new ArrayList<>();
    List<RichBazelEdge> usedBazelDependencyEdges = new ArrayList<>();
    List<RichBazelEdge> unusedBazelDependencyEdges = new ArrayList<>();
    for (BazelEdge edge : allBazelDependencyEdges) {
      RichBazelEdge richEdge = makeRichBazelEdge(edge);
      if (shouldIgnoreEdge(edge)) {
        ignoredBazelDependencyEdges.add(richEdge);
      } else if (actualBazelDependencyEdges.contains(edge)) {
        usedBazelDependencyEdges.add(richEdge);
      } else {
        unusedBazelDependencyEdges.add(richEdge);
      }
    }
    Instant endTime = Instant.now();
    return UnusedDepsAnalysisResult.create(
        ignoredBazelDependencyEdges,
        usedBazelDependencyEdges,
        unusedBazelDependencyEdges,
        Duration.between(startTime, endTime));
  }

  private Set<BazelEdge> getActualBazelDependencyEdges(
      List<JavaFileImportedSymbol> symbolImportEdges) {
    return symbolImportEdges.stream()
        .map(this::getActualBazelDependencyEdges)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Gets the actual Bazel target dependency edges resulting from an import of a symbol from one
   * Java source file to another. There could be multiple edges if the Java source file either
   * importing or exporting the symbol is in multiple Bazel targets.
   *
   * @param symbolImportEdge the edge capturing the imported symbol and the importer file
   * @return the Bazel target dependency edges
   */
  private Set<BazelEdge> getActualBazelDependencyEdges(JavaFileImportedSymbol symbolImportEdge) {
    Set<BazelEdge> actualBazelDependencyEdges = new HashSet<>();
    JavaFile importerFile = getJavaFile(symbolImportEdge.getFileId());

    Set<BazelTarget> importerTargets = getBazelTargetsForJavaFile(importerFile);
    Set<BazelTarget> exporterTargets = getTargetsThatExportSymbol(symbolImportEdge);
    Set<BazelTarget> transitiveExporterTargets =
        getAllExportersThatExportTargetTransitively(exporterTargets);

    /* add 'actual' edges from all Bazel targets for importer file to all Bazel
    targets for exporter file */
    for (BazelTarget importerTarget : importerTargets) {
      /* We should only add edges from the importer target to exporter targets if there was not
      a self-edge. So if we had libA(srcs = [A.java, B.java]) and libB(srcs = [B.java, C.java])
      where A.java imports the symbol 'Foo' from B.java, we should not add an edge from libA to libB
      even though libB is a target that exports 'Foo', because libA also exports 'Foo'. This means there
      is a self-edge between libA and itself, so the edge from libA to libB is unnecessary */
      boolean existsSelfEdge = false;
      Set<BazelEdge> potentialEdges = new HashSet<>();
      for (BazelTarget exporterTarget : exporterTargets) {
        if (Objects.equals(importerTarget, exporterTarget)) {
          existsSelfEdge = true;
          break;
        } else {
          potentialEdges.add(
              new BazelEdge(importerTarget.getTargetId(), exporterTarget.getTargetId()));
        }
      }
      for (BazelTarget transitiveExporterTarget : transitiveExporterTargets) {
        potentialEdges.add(
            new BazelEdge(importerTarget.getTargetId(), transitiveExporterTarget.getTargetId()));
      }
      if (!existsSelfEdge) {
        actualBazelDependencyEdges.addAll(potentialEdges);
      }
    }
    return actualBazelDependencyEdges;
  }

  /**
   * For a given symbol import, finds the Bazel targets that directly export that symbol.
   *
   * @param importEdge an edge connecting a Java file and a symbol it imports
   * @return the Bazel targets that export the symbol
   */
  private Set<BazelTarget> getTargetsThatExportSymbol(JavaFileImportedSymbol importEdge) {
    Set<BazelTarget> symbolExporterTargets;
    if (symbolIdToJavaFileExportedSymbol.containsKey(importEdge.getSymbolId())) {
      // imported symbol was exported by another first-party file
      JavaFileExportedSymbol exportEdge =
          symbolIdToJavaFileExportedSymbol.get(importEdge.getSymbolId());
      JavaFile exporterFile = getJavaFile(exportEdge.getFileId());
      symbolExporterTargets = getBazelTargetsForJavaFile(exporterFile);
    } else {
      // symbol is defined in third party library
      Symbol symbol = getSymbol(importEdge.getSymbolId());
      Optional<ThirdPartySymbol> thirdPartySymbol =
          thirdPartySymbolsIndex.getSymbol(symbol.getFullyQualifiedName());
      symbolExporterTargets =
          thirdPartySymbol
              .flatMap(partySymbol -> getBazelTarget(partySymbol.getTarget()))
              .map(Set::of)
              .orElseGet(Set::of);
    }
    return symbolExporterTargets;
  }

  private Set<BazelTarget> getAllExportersThatExportTargetTransitively(Set<BazelTarget> targets) {
    Set<BazelTarget> exporters = new HashSet<>();
    ArrayDeque<BazelTarget> next = new ArrayDeque<>(targets);
    while (!next.isEmpty()) {
      BazelTarget curr = next.pop();
      if (!targets.contains(curr)) {
        exporters.add(curr);
      }
      next.addAll(getExportersOfTarget(curr));
    }
    return exporters;
  }

  private boolean shouldIgnoreEdge(BazelEdge edge) {
    BazelTarget fromTarget = getBazelTarget(edge.getFromTargetId());
    BazelTarget toTarget = getBazelTarget(edge.getToTargetId());
    return isThirdPartyTarget(fromTarget) // don't consider dependencies of 3rd party targets
        || !targetFilter.matches(fromTarget) // don't consider targets that don't satisfy filter
        || shouldIgnoreTarget(fromTarget) // don't consider edges involving an ignored target
        || shouldIgnoreTarget(toTarget);
  }

  private static boolean isThirdPartyTarget(BazelTarget target) {
    return target.getTargetLabel().startsWith("@maven");
  }

  private static boolean shouldIgnoreTarget(BazelTarget target) {
    // ex: label //src/example/path/dir:lib -> cleaned //src/example/path/dir
    String cleaned = target.getTargetLabel().replaceAll(":.+", "");
    return DEPENDENCY_ANALYZER_AND_ITS_DEPS.contains(cleaned)
        || IGNORED_DEPS.contains(cleaned)
        || IGNORED_DEPS.contains(target.getTargetLabel());
  }

  private BazelTarget getBazelTarget(int targetId) {
    return Objects.requireNonNull(targetIdToBazelTarget.get(targetId));
  }

  private Optional<BazelTarget> getBazelTarget(String targetLabel) {
    return Optional.ofNullable(targetLabelToBazelTarget.get(targetLabel));
  }

  private JavaFile getJavaFile(int fileId) {
    return Objects.requireNonNull(fileIdToJavaFile.get(fileId));
  }

  private Symbol getSymbol(int symbolId) {
    return Objects.requireNonNull(symbolIdToSymbol.get(symbolId));
  }

  private Set<BazelTarget> getBazelTargetsForJavaFile(JavaFile javaFile) {
    return Objects.requireNonNull(javaFileToBazelTargets.get(javaFile));
  }

  /**
   * Gets the set of Bazel targets that export a given target via the 'exports' Bazel attribute.
   *
   * @param exportedTarget the target being exported
   * @return the set of any targets that export the given target
   */
  private Set<BazelTarget> getExportersOfTarget(BazelTarget exportedTarget) {
    return exportedTargetIdToExporterTargets.getOrDefault(
        exportedTarget.getTargetId(), new HashSet<>());
  }

  private Map<Integer, BazelTarget> getAllBazelTargetsById() {
    return database.getAllBazelTargets().stream()
        .collect(Collectors.toMap(BazelTarget::getTargetId, target -> target));
  }

  private Map<String, BazelTarget> getAllBazelTargetsByLabel() {
    return database.getAllBazelTargets().stream()
        .collect(Collectors.toMap(BazelTarget::getTargetLabel, target -> target));
  }

  private Map<Integer, Set<BazelTarget>> getExportedTargetIdToExporterTargets() {
    Map<Integer, Set<BazelTarget>> map = new HashMap<>();
    for (BazelExportEdge edge : database.getAllBazelExportEdges()) {
      map.computeIfAbsent(edge.getExportedTargetId(), k -> new HashSet<>())
          .add(getBazelTarget(edge.getExporterTargetId()));
    }
    return map;
  }

  private Map<Integer, JavaFile> getAllJavaFilesById() {
    return database.getAllJavaFiles().stream()
        .collect(Collectors.toMap(JavaFile::getFileId, file -> file));
  }

  private Map<Integer, Symbol> getAllSymbolsById() {
    return database.getAllSymbols().stream()
        .collect(Collectors.toMap(Symbol::getSymbolId, symbol -> symbol));
  }

  private Map<Integer, JavaFileExportedSymbol> getJavaFileExportedSymbolsBySymbolId() {
    Map<Integer, JavaFileExportedSymbol> map = new HashMap<>();
    for (var exportEdge : database.getAllJavaFileExportedSymbols()) {
      map.put(exportEdge.getSymbolId(), exportEdge);
    }
    return map;
  }

  private Map<JavaFile, Set<BazelTarget>> getJavaFileToBazelTargets() {
    Map<JavaFile, Set<BazelTarget>> javaFileToBazelTargets = new HashMap<>();
    for (JavaFileBazelTarget edge : database.getAllJavaFileBazelTargets()) {
      JavaFile file = getJavaFile(edge.getFileId());
      BazelTarget target = getBazelTarget(edge.getTargetId());
      javaFileToBazelTargets.putIfAbsent(file, new HashSet<>());
      Objects.requireNonNull(javaFileToBazelTargets.get(file)).add(target);
    }
    return javaFileToBazelTargets;
  }

  private RichBazelEdge makeRichBazelEdge(BazelEdge edge) {
    return RichBazelEdge.create(
        getBazelTarget(edge.getFromTargetId()), getBazelTarget(edge.getToTargetId()));
  }
}
