package com.stripe.build.dependencyanalyzer.bazel;

import static java.util.function.Predicate.not;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/** A wrapper around a string representing a directory */
@AutoValue
public abstract class Directory {
  public abstract String value();

  /** Create a directory without validation from the given string value */
  public static Directory of(String value) {
    return new AutoValue_Directory(value);
  }

  /** Parse a location of the format {@code <file>:<row>:<col>} into a Directory */
  public static Optional<Directory> parse(String location, String rootDir) {
    if (!location.startsWith(rootDir)) {
      return Optional.empty();
    }

    var path = Splitter.on(':').splitToList(location.substring(rootDir.length()));

    if (path.size() != 3) {
      // We've a label that's not on the format `/path/to/our/target/BUILD:row:col`
      return Optional.empty();
    }

    var parts = path.get(0).split("/");
    var dir =
        Arrays.stream(parts)
            .filter(not(String::isEmpty))
            .filter(part -> !part.startsWith("BUILD"))
            .collect(Collectors.joining("/"));

    return Optional.of(of(dir));
  }
}
