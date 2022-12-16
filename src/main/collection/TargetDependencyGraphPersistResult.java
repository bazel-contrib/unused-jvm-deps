package com.stripe.build.dependencyanalyzer.collection;

import com.google.auto.value.AutoValue;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.commons.io.FileUtils;

@AutoValue
public abstract class TargetDependencyGraphPersistResult {
  public abstract Path getOutputFile();

  public abstract int getNumTargetsInserted();

  public abstract int getNumEdgesInserted();

  public abstract long getFileSize();

  public abstract Duration getTimeElapsed();

  public static TargetDependencyGraphPersistResult create(
      Path outputFile,
      int numTargetsInserted,
      int numEdgesInserted,
      long fileSize,
      Duration timeElapsed) {
    return new AutoValue_TargetDependencyGraphPersistResult(
        outputFile, numTargetsInserted, numEdgesInserted, fileSize, timeElapsed);
  }

  @Override
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "Persisted graph to: %s (size %s)\n",
            getOutputFile().toString(), FileUtils.byteCountToDisplaySize(getFileSize())));
    sb.append(String.format("Inserted %d targets\n", getNumTargetsInserted()));
    sb.append(String.format("Inserted %d edges\n", getNumEdgesInserted()));
    sb.append(String.format("Time elapsed: %d seconds\n", getTimeElapsed().getSeconds()));
    return sb.toString();
  }
}
