package com.stripe.build.dependencyanalyzer.analysis;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
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

  private final Path bazelWorkspace;

  public ThirdPartySymbolsIndex(Path bazelWorkspace) throws IOException {
    this.bazelWorkspace = bazelWorkspace;
  }

  public Optional<ThirdPartySymbol> getSymbol(String fullyQualifiedName) {
    return Optional.empty();
  }
}
