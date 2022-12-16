package com.stripe.build.dependencyanalyzer.collection;

import com.google.auto.value.AutoValue;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.commons.io.FileUtils;

@AutoValue
public abstract class SymbolsPersistResult {
  public abstract int getNumJavaSourceFilesInserted();

  public abstract int getNumUniqueSymbolsInserted();

  public abstract int getNumExportedSymbolsInserted();

  public abstract int getNumImportedSymbolsInserted();

  public abstract Path getOutputFile();

  public abstract long getFileSize();

  public abstract Duration getTimeElapsed();

  public static SymbolsPersistResult create(
      int numJavaSourceFilesInserted,
      int numUniqueSymbolsInserted,
      int numExportedSymbolsInserted,
      int numImportedSymbolsInserted,
      Path outputFile,
      long fileSize,
      Duration timeElapsed) {
    return new AutoValue_SymbolsPersistResult(
        numJavaSourceFilesInserted,
        numUniqueSymbolsInserted,
        numExportedSymbolsInserted,
        numImportedSymbolsInserted,
        outputFile,
        fileSize,
        timeElapsed);
  }

  @Override
  public final String toString() {
    return new StringBuilder()
        .append(
            String.format(
                "Persisted symbols to: %s (size %s)\n",
                getOutputFile().toString(), FileUtils.byteCountToDisplaySize(getFileSize())))
        .append(String.format("Inserted %d Java source files\n", getNumJavaSourceFilesInserted()))
        .append(String.format("Inserted %d unique symbols\n", getNumUniqueSymbolsInserted()))
        .append(
            String.format(
                "Inserted %d edges between Java source files and exported symbols\n",
                getNumExportedSymbolsInserted()))
        .append(
            String.format(
                "Inserted %d edges between Java source files and imported symbols\n",
                getNumImportedSymbolsInserted()))
        .append(String.format("Time elapsed: %d seconds\n", getTimeElapsed().getSeconds()))
        .toString();
  }
}
