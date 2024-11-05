package com.stripe.build.dependencyanalyzer.analysis;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores the parsed third-party symbol index provided by build-helper which can be used to map a
 * third-party symbol to its Bazel target.
 */
public class ThirdPartySymbolsIndex {

  /**
   * Complete list of targets that are either not present in the third-party index or incompletely
   * indexed, meaning we should not remove dependencies on these targets.
   */
  public static final ImmutableSet<String> ALL_EXCLUDED_TARGETS =
      ImmutableSet.<String>builder().build();

  private final Map<String, ThirdPartySymbol> fullyQualifiedNameToSymbol = new HashMap<>();

  @Nullable private final Path indexPath;
  private final Character delimiter;

  public ThirdPartySymbolsIndex(@Nullable Path thirdPartyIndexPath, Character delimiter)
      throws IOException {
    this.indexPath = thirdPartyIndexPath;
    this.delimiter = delimiter;
    loadIndex();
  }

  public Optional<ThirdPartySymbol> getSymbol(String fullyQualifiedName) {
    return Optional.ofNullable(this.fullyQualifiedNameToSymbol.get(fullyQualifiedName));
  }

  private void loadIndex() {
    if (this.indexPath == null) {
      return;
    }

    InputStream inputStream = null;
    try {
      inputStream = Files.newInputStream(this.indexPath);
    } catch (IOException e) {
      System.err.printf("Failed to find third party index file at %s%n", this.indexPath);
      return;
    }

    try (CSVParser parser =
        CSVParser.parse(
            inputStream,
            StandardCharsets.UTF_8,
            CSVFormat.DEFAULT.withDelimiter(this.delimiter).withFirstRecordAsHeader())) {
      List<CSVRecord> allRecords = parser.getRecords();
      for (CSVRecord record: allRecords) {
        ThirdPartySymbol symbol =
                ThirdPartySymbol.create(
                        StringUtils.defaultIfBlank(record.get("fqn"), "").replaceAll("\\$", "."),
                        record.get("target"),
                        record.get("language"),
                        record.get("language_visibility"),
                        record.get("bazel_visibility"));
        fullyQualifiedNameToSymbol.put(symbol.getFullyQualifiedName(), symbol);
      }
    } catch (IOException e) {
      System.err.printf("Failed to read and parse third party index file at %s%n", this.indexPath);
    }
  }
}
