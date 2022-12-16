package com.stripe.build.dependencyanalyzer.codegen;

import com.google.common.io.Resources;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import org.apache.commons.lang3.StringUtils;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;

/**
 * Class that uses Jooq to generate Java source code (Tables, Records, POJOs, etc.) from the
 * DependencyAnalyzer database schema, which makes interacting with the database a lot nicer and is
 * the main benefit of Jooq.
 */
public class JooqCodeGenerator {
  private static final String TEMP_DIR_PREFIX = "generate-code";
  private static final String TEMP_DB_NAME = "tables.db";

  private static final String DRIVER = "org.sqlite.JDBC";
  private static final String JOOQ_DB = "org.jooq.meta.sqlite.SQLiteDatabase";
  private static final String JOOQ_GENERATOR = "org.jooq.codegen.JavaGenerator";
  private static final String GENERATED_PACKAGE_NAME =
      "com.stripe.build.dependencyanalyzer.database.generated";

  /**
   * Runs a SQL script to create SQLite database schema, then uses Jooq to generate Java source code
   * to represent that schema.
   *
   * @param outputSrcJarPath absolute path to the root Java source directory; for example:
   *     /Users/me/my/repo/src/main/java
   */
  private static void generateJavaCode(Path outputSrcJarPath) throws Exception {
    Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
    String databaseUrl =
        SqlScriptRunner.SQLITE_PREFIX + tempDir.toAbsolutePath() + "/" + TEMP_DB_NAME;
    try (Connection connection = DriverManager.getConnection(databaseUrl)) {
      // initialize database tables in temporary directory, so that Jooq can autogenerate Java code
      String script =
          Resources.toString(
              Resources.getResource(SqlScriptRunner.SQL_SCRIPT_RESOURCE), StandardCharsets.UTF_8);
      SqlScriptRunner.execute(connection, script);

      Jdbc jdbc = new Jdbc().withDriver(DRIVER).withUrl(databaseUrl);
      Generate generate =
          new Generate().withPojos(true).withFluentSetters(true).withPojosEqualsAndHashCode(true);
      Database database = new Database().withName(JOOQ_DB);
      Target target =
          new Target()
              .withPackageName(GENERATED_PACKAGE_NAME)
              .withDirectory(tempDir.toAbsolutePath().toString());

      Generator generator =
          new Generator()
              .withName(JOOQ_GENERATOR)
              .withGenerate(generate)
              .withDatabase(database)
              .withTarget(target);
      Configuration configuration = new Configuration().withJdbc(jdbc).withGenerator(generator);
      GenerationTool.generate(configuration);

      // write generated Java source files to a JAR
      try (Stream<Path> srcStream = Files.walk(tempDir)) {
        List<Path> srcPaths =
            srcStream
                .filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toList());

        try (OutputStream os = Files.newOutputStream(outputSrcJarPath);
            JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(os))) {
          for (Path srcPath : srcPaths) {
            Path relativePath = tempDir.relativize(srcPath);
            String fileContent = Files.readString(srcPath);
            byte[] contentBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            JarEntry jarEntry = new JarEntry(relativePath.toString());
            jarEntry.setTime(System.currentTimeMillis());
            jarEntry.setSize(fileContent.length());
            jarEntry.setMethod(ZipEntry.DEFLATED);

            jarStream.putNextEntry(jarEntry);
            jarStream.write(contentBytes, 0, contentBytes.length);
            jarStream.closeEntry();
          }
        }
      }
    } finally {
      tempDir.toFile().deleteOnExit();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1 || StringUtils.isBlank(args[0])) {
      System.out.println("Usage: generate-code <outputSrcJarPath>");
    } else {
      generateJavaCode(Path.of(args[0]));
    }
  }
}
