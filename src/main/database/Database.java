package com.stripe.build.dependencyanalyzer.database;

import static com.stripe.build.dependencyanalyzer.database.generated.Tables.BAZEL_EDGE;
import static com.stripe.build.dependencyanalyzer.database.generated.Tables.BAZEL_EXPORT_EDGE;
import static com.stripe.build.dependencyanalyzer.database.generated.Tables.BAZEL_TARGET;
import static com.stripe.build.dependencyanalyzer.database.generated.Tables.JAVA_FILE;
import static com.stripe.build.dependencyanalyzer.database.generated.Tables.JAVA_FILE_BAZEL_TARGET;
import static com.stripe.build.dependencyanalyzer.database.generated.Tables.JAVA_FILE_EXPORTED_SYMBOL;
import static com.stripe.build.dependencyanalyzer.database.generated.Tables.JAVA_FILE_IMPORTED_SYMBOL;
import static com.stripe.build.dependencyanalyzer.database.generated.Tables.SYMBOL;

import com.google.common.io.Resources;
import com.stripe.build.dependencyanalyzer.codegen.SqlScriptRunner;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelEdge;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelExportEdge;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.BazelTarget;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFile;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileBazelTarget;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileExportedSymbol;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.JavaFileImportedSymbol;
import com.stripe.build.dependencyanalyzer.database.generated.tables.pojos.Symbol;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

/** Handles the connection to the SQLite database storing dependency graph tables. */
public class Database implements AutoCloseable {

  private final Path filePath;
  private final Connection connection;

  public static Database createNew(Path filePath) throws IOException, SQLException {
    if (Files.exists(filePath)) {
      Files.delete(filePath); // delete database if already exists
    }
    var database = new Database(filePath);
    database.createTables();
    return database;
  }

  public static Database open(Path filePath) throws SQLException {
    return new Database(filePath);
  }

  private Database(Path filePath) throws SQLException {
    this.filePath = filePath.toAbsolutePath();
    connection = DriverManager.getConnection(SqlScriptRunner.SQLITE_PREFIX + this.filePath);
  }

  @Override
  public void close() throws SQLException {
    connection.close();
  }

  public void bulkInsertBazelTargets(Collection<BazelTarget> bazelTargets) {
    wrapBulkInsertionInTransaction(
        bazelTargets,
        (DSLContext context, BazelTarget target) ->
            context
                .insertInto(BAZEL_TARGET, BAZEL_TARGET.TARGET_ID, BAZEL_TARGET.TARGET_LABEL)
                .values(target.getTargetId(), target.getTargetLabel())
                .execute());
  }

  public void bulkInsertBazelEdges(Collection<BazelEdge> bazelEdges) {
    wrapBulkInsertionInTransaction(
        bazelEdges,
        (DSLContext context, BazelEdge edge) ->
            context
                .insertInto(BAZEL_EDGE, BAZEL_EDGE.FROM_TARGET_ID, BAZEL_EDGE.TO_TARGET_ID)
                .values(edge.getFromTargetId(), edge.getToTargetId())
                .execute());
  }

  public void bulkInsertExportEdges(Collection<BazelExportEdge> exportEdges) {
    wrapBulkInsertionInTransaction(
        exportEdges,
        (DSLContext context, BazelExportEdge exportEdge) ->
            context
                .insertInto(
                    BAZEL_EXPORT_EDGE,
                    BAZEL_EXPORT_EDGE.EXPORTER_TARGET_ID,
                    BAZEL_EXPORT_EDGE.EXPORTED_TARGET_ID)
                .values(exportEdge.getExporterTargetId(), exportEdge.getExportedTargetId())
                .execute());
  }

  public void bulkInsertJavaFiles(Collection<JavaFile> filesToInsert) {
    wrapBulkInsertionInTransaction(
        filesToInsert,
        (DSLContext context, JavaFile file) ->
            context
                .insertInto(JAVA_FILE, JAVA_FILE.FILE_ID, JAVA_FILE.FILE_PATH)
                .values(file.getFileId(), file.getFilePath())
                .execute());
  }

  public void bulkInsertJavaFileBazelTargets(Collection<JavaFileBazelTarget> edges) {
    wrapBulkInsertionInTransaction(
        edges,
        (DSLContext context, JavaFileBazelTarget edge) ->
            context
                .insertInto(
                    JAVA_FILE_BAZEL_TARGET,
                    JAVA_FILE_BAZEL_TARGET.FILE_ID,
                    JAVA_FILE_BAZEL_TARGET.TARGET_ID)
                .values(edge.getFileId(), edge.getTargetId())
                .execute());
  }

  public void bulkInsertSymbols(Collection<Symbol> symbols) {
    wrapBulkInsertionInTransaction(
        symbols,
        (DSLContext context, Symbol symbol) ->
            context
                .insertInto(SYMBOL, SYMBOL.SYMBOL_ID, SYMBOL.FULLY_QUALIFIED_NAME)
                .values(symbol.getSymbolId(), symbol.getFullyQualifiedName())
                .execute());
  }

  public void bulkInsertJavaFileExportedSymbol(Collection<JavaFileExportedSymbol> edges) {
    wrapBulkInsertionInTransaction(
        edges,
        (DSLContext context, JavaFileExportedSymbol edge) ->
            context
                .insertInto(
                    JAVA_FILE_EXPORTED_SYMBOL,
                    JAVA_FILE_EXPORTED_SYMBOL.FILE_ID,
                    JAVA_FILE_EXPORTED_SYMBOL.SYMBOL_ID)
                .values(edge.getFileId(), edge.getSymbolId())
                .execute());
  }

  public void bulkInsertJavaFileImportedSymbol(Collection<JavaFileImportedSymbol> edges) {
    wrapBulkInsertionInTransaction(
        edges,
        (DSLContext context, JavaFileImportedSymbol edge) ->
            context
                .insertInto(
                    JAVA_FILE_IMPORTED_SYMBOL,
                    JAVA_FILE_IMPORTED_SYMBOL.FILE_ID,
                    JAVA_FILE_IMPORTED_SYMBOL.SYMBOL_ID)
                .values(edge.getFileId(), edge.getSymbolId())
                .execute());
  }

  private <T> void wrapBulkInsertionInTransaction(
      Collection<T> objectsToInsert, BiConsumer<DSLContext, T> insertOneObject) {
    getDSLContext()
        .transaction(
            (Configuration trx) -> {
              DSLContext context = trx.dsl();
              for (T object : objectsToInsert) {
                insertOneObject.accept(context, object);
              }
            });
  }

  public List<BazelTarget> getAllBazelTargets() {
    return getAllRowsInTable(BAZEL_TARGET, BazelTarget.class);
  }

  public List<BazelEdge> getAllBazelEdges() {
    return getAllRowsInTable(BAZEL_EDGE, BazelEdge.class);
  }

  public List<BazelExportEdge> getAllBazelExportEdges() {
    return getAllRowsInTable(BAZEL_EXPORT_EDGE, BazelExportEdge.class);
  }

  public List<JavaFile> getAllJavaFiles() {
    return getAllRowsInTable(JAVA_FILE, JavaFile.class);
  }

  public List<Symbol> getAllSymbols() {
    return getAllRowsInTable(SYMBOL, Symbol.class);
  }

  public List<JavaFileBazelTarget> getAllJavaFileBazelTargets() {
    return getAllRowsInTable(JAVA_FILE_BAZEL_TARGET, JavaFileBazelTarget.class);
  }

  public List<JavaFileExportedSymbol> getAllJavaFileExportedSymbols() {
    return getAllRowsInTable(JAVA_FILE_EXPORTED_SYMBOL, JavaFileExportedSymbol.class);
  }

  public List<JavaFileImportedSymbol> getAllJavaFileImportedSymbols() {
    return getAllRowsInTable(JAVA_FILE_IMPORTED_SYMBOL, JavaFileImportedSymbol.class);
  }

  public Path getFilePath() {
    return filePath;
  }

  public long getFileSize() throws IOException {
    return Files.size(filePath);
  }

  /**
   * Create tables using the same SQL script used to create the tables from which the Java source
   * code was generated.
   *
   * @throws IOException if error occurs loading SQL script resource
   * @throws SQLException if error occurs executing SQL script
   */
  public void createTables() throws IOException, SQLException {
    String script =
        Resources.toString(
            Resources.getResource(SqlScriptRunner.SQL_SCRIPT_RESOURCE), StandardCharsets.UTF_8);
    SqlScriptRunner.execute(connection, script);
  }

  private <R extends Record, C> List<C> getAllRowsInTable(TableImpl<R> table, Class<C> clazz) {
    return getDSLContext().select().from(table).fetch().stream()
        .map(r -> r.into(clazz))
        .collect(Collectors.toList());
  }

  private DSLContext getDSLContext() {
    return DSL.using(connection, SQLDialect.SQLITE);
  }
}
