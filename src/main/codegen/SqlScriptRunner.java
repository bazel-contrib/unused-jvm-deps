package com.stripe.build.dependencyanalyzer.codegen;

import com.google.common.base.Splitter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to run a simple SQL script because JDBC and Jooq do not have a way to execute SQL scripts
 * from Java.
 */
public class SqlScriptRunner {

  public static final String SQLITE_PREFIX = "jdbc:sqlite:";
  public static final String SQL_SCRIPT_RESOURCE = "resources/database/create_tables.sql";

  // based on https://www.sqlite.org/syntax/comment-syntax.html
  private static final Pattern COMMENT_PATTERN =
      Pattern.compile("(--.*\\n)|((/\\*)(.|\\n)*?(\\*/))");

  /**
   * Executes a SQL script String statement by statement, separating on ';'.
   *
   * @param connection the database connection
   * @param script String of SQL script
   * @throws SQLException if error occurs
   */
  public static void execute(Connection connection, String script) throws SQLException {
    String removedComments = COMMENT_PATTERN.matcher(script).replaceAll("");
    Iterable<String> dirtyStatements = Splitter.on(';').split(removedComments);
    for (String dirty : dirtyStatements) {
      String removedNewLine = dirty.trim().replace("\n", " ");
      if (StringUtils.isBlank(removedNewLine)) {
        continue;
      }
      String statement = removedNewLine + ";";
      try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
        preparedStatement.execute();
      }
    }
  }
}
