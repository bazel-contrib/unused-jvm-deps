package com.stripe.build.dependencyanalyzer.bazel;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AutoValue
public abstract class BazelFileLabel {
  public abstract String value();

  // Given a string, determine if this is a file label and if so create one:
  public static Optional<BazelFileLabel> of(String value) {
    var matcher = Pattern.compile("^//.+\\.\\w+").matcher(value);

    if (!matcher.matches()) {
      return Optional.empty();
    }

    return Optional.of(new AutoValue_BazelFileLabel(value));
  }

  /** Create a {@link BazelFileLabel} from a file path */
  public static Optional<BazelFileLabel> fromPath(String filePath) {
    var parts = Splitter.on('/').splitToList(filePath);

    if (parts.size() == 0) {
      return Optional.empty();
    }

    var fileName = parts.get(parts.size() - 1);

    if (parts.size() == 1) {
      return of("//:" + fileName);
    }

    var fileLabel =
        parts.stream().limit(parts.size() - 1).collect(Collectors.joining("/", "//", ":"))
            + fileName;

    return of(fileLabel);
  }

  public Directory toDirectory() {
    var matcher = Pattern.compile("^\\/\\/([^:]+)?:.+$").matcher(value());
    // Call to `Matcher#matches` to actually use the matcher on the input value:
    matcher.matches();
    try {
      // if the first group is empty, the file is the root of the repo
      // for example, `//:mergeq.txt`
      return Directory.of(Optional.ofNullable(matcher.group(1)).orElse("/"));
    } catch (IllegalStateException e) {
      throw new IllegalStateException(
          String.format("Failed to turn \"%s\" into a directory", this), e);
    }
  }
}
