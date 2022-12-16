package com.stripe.build.dependencyanalyzer.collection;

import com.stripe.build.dependencyanalyzer.database.Database;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelTarget;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFile;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileBazelTarget;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileExportedSymbol;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileImportedSymbol;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.Symbol;
import com.stripe.build.dependencyanalyzer.plugin.SymbolCollectionResult;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SymbolsPersister {

  private final Database database;

  /**
   * Persists the information stored in the symbols.json files gathered and passed in to this method
   * via symbolsFiles to the database. The basic mapping being stored in this step is the
   * many-to-many relationships between Java files and imported/exported symbols, as well as Java
   * files and Bazel targets.
   *
   * @param symbolsFiles all the gathered symbols files parsed and loaded into memory
   * @param database the database to persist the information in
   * @return a results object containing stats about the database
   * @throws IOException if error getting the size of the database
   */
  public static SymbolsPersistResult persistSymbols(
      SymbolsFileGatherResult symbolsFiles, Database database) throws IOException {
    return new SymbolsPersister(database).persist(symbolsFiles);
  }

  private SymbolsPersister(Database database) {
    this.database = database;
  }

  private SymbolsPersistResult persist(SymbolsFileGatherResult symbolsFileGatherResult)
      throws IOException {
    Instant startTime = Clock.systemUTC().instant();

    Map<String, BazelTarget> labelToBazelTarget =
        database.getAllBazelTargets().stream()
            .collect(Collectors.toMap(BazelTarget::getTargetLabel, target -> target));

    Set<Symbol> symbolsToInsert = new HashSet<>();
    int nextSymbolId = 1;
    for (String fullyQualifiedName : symbolsFileGatherResult.getAllSymbols()) {
      symbolsToInsert.add(new Symbol(nextSymbolId++, fullyQualifiedName));
    }
    database.bulkInsertSymbols(symbolsToInsert);
    symbolsToInsert.clear();

    Map<String, Symbol> symbolNameToSymbol =
        database.getAllSymbols().stream()
            .collect(Collectors.toMap(Symbol::getFullyQualifiedName, symbol -> symbol));

    int nextFileId = 1;
    Map<String, JavaFile> fileNameToJavaFile = new HashMap<>();
    Set<JavaFileBazelTarget> javaFileBazelTargetsToInsert = new HashSet<>();
    Set<JavaFileExportedSymbol> javaFileExportedSymbolsToInsert = new HashSet<>();
    Set<JavaFileImportedSymbol> javaFileImportedSymbolsToInsert = new HashSet<>();
    for (SymbolCollectionResult symbolsFile : symbolsFileGatherResult.getSymbolResults()) {
      BazelTarget bazelTarget =
          Objects.requireNonNull(labelToBazelTarget.get(symbolsFile.getBazelTargetLabel()));

      JavaFile javaFile =
          fileNameToJavaFile.getOrDefault(
              symbolsFile.getSourceFileName(),
              new JavaFile(nextFileId++, symbolsFile.getSourceFileName()));
      fileNameToJavaFile.putIfAbsent(javaFile.getFilePath(), javaFile);
      javaFileBazelTargetsToInsert.add(
          new JavaFileBazelTarget(bazelTarget.getTargetId(), javaFile.getFileId()));

      // persist the edges between Java files and the symbols they export
      symbolsFile.getExportedSymbols().stream()
          .map(s -> Objects.requireNonNull(symbolNameToSymbol.get(s)))
          .forEach(
              symbol ->
                  javaFileExportedSymbolsToInsert.add(
                      new JavaFileExportedSymbol(javaFile.getFileId(), symbol.getSymbolId())));

      // persist the edges between Java files and the symbols they import
      symbolsFile.getImportedSymbols().stream()
          .map(s -> Objects.requireNonNull(symbolNameToSymbol.get(s)))
          .forEach(
              symbol ->
                  javaFileImportedSymbolsToInsert.add(
                      new JavaFileImportedSymbol(javaFile.getFileId(), symbol.getSymbolId())));
    }
    database.bulkInsertJavaFiles(fileNameToJavaFile.values());
    database.bulkInsertJavaFileBazelTargets(javaFileBazelTargetsToInsert);
    database.bulkInsertJavaFileExportedSymbol(javaFileExportedSymbolsToInsert);
    database.bulkInsertJavaFileImportedSymbol(javaFileImportedSymbolsToInsert);

    Instant endTime = Clock.systemUTC().instant();
    return SymbolsPersistResult.create(
        database.getAllJavaFiles().size(),
        database.getAllSymbols().size(),
        database.getAllJavaFileExportedSymbols().size(),
        database.getAllJavaFileImportedSymbols().size(),
        database.getFilePath(),
        database.getFileSize(),
        Duration.between(startTime, endTime));
  }
}
