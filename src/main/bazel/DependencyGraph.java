package com.stripe.build.dependencyanalyzer.bazel;

import static java.util.stream.Collectors.toSet;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This dependency graph is read from the output of a Bazel query like:
 *
 * <p>{@code bazel query //... --output=proto > targets.proto}
 */
public class DependencyGraph {
  private static final Logger log = LoggerFactory.getLogger(DependencyGraph.class);

  final Map<Directory, Set<BazelRuleLabel>> locationToRule = new HashMap<>();
  final Map<BazelRuleLabel, Rule> nameToRule = new HashMap<>();
  final Map<BazelFileLabel, Set<BazelRuleLabel>> labelToName = new HashMap<>();

  private DependencyGraph() {}

  /** Parse a dependency graph from the given input stream */
  public static DependencyGraph fromProtoInputStream(InputStream is) throws IOException {
    var result = Build.QueryResult.parseFrom(is);
    return fromBuildResult(result);
  }

  /** Parse a dependency graph from the given {@link Build.QueryResult} */
  public static DependencyGraph fromBuildResult(Build.QueryResult result) {
    return fromBuildResult(result, "/src");
  }

  /** Parse a dependency graph from the given {@link Build.QueryResult} */
  public static DependencyGraph fromBuildResult(Build.QueryResult result, String rootDir) {
    var graph = new DependencyGraph();
    graph.parseResult(result, rootDir);
    return graph;
  }

  public Set<Rule> getAllRules() {
    return ImmutableSet.copyOf(nameToRule.values());
  }

  public Optional<Rule> getRuleWithLabel(BazelRuleLabel label) {
    return nameToRule.containsKey(label) ? Optional.of(nameToRule.get(label)) : Optional.empty();
  }

  private void parseResult(Build.QueryResult result, String rootDir) {
    for (var target : result.getTargetList()) {
      if (!target.hasRule()) {
        continue;
      }
      var targetRule = target.getRule();

      var name = BazelRuleLabel.of(targetRule.getName());
      var inputFiles =
          targetRule.getRuleInputList().stream()
              .flatMap(r -> BazelFileLabel.of(r).stream())
              .collect(toSet());

      // Put the rule into the location map and the label map:
      var parsedDir = Directory.parse(targetRule.getLocation(), rootDir);

      if (parsedDir.isEmpty()) {
        log.error("DEPENDENCY-GRAPH-DIR-PARSE-ERROR name={}", name);
        continue;
      }

      var location = parsedDir.get();

      var inputRules =
          targetRule.getRuleInputList().stream()
              .flatMap(r -> BazelRuleLabel.fromInputLabel(location, r).stream())
              .filter(l -> l.value().startsWith("//"))
              .collect(toSet());

      var rule = Rule.of(name, inputFiles, inputRules);

      // Add rule to directory:
      this.locationToRule.putIfAbsent(location, new HashSet<>());
      this.locationToRule.getOrDefault(location, Set.of()).add(name);

      this.nameToRule.put(rule.name(), rule);

      // Add input file:
      inputFiles.forEach(
          f -> {
            this.labelToName.putIfAbsent(f, new HashSet<>());
            this.labelToName.getOrDefault(f, Set.of()).add(name);
          });
    }

    System.out.println("Finished parsing");
  }

  @AutoValue
  public abstract static class Rule {
    public abstract BazelRuleLabel name();

    public abstract ImmutableSet<BazelFileLabel> inputFiles();

    public abstract ImmutableSet<BazelRuleLabel> inputRules();

    private static Rule of(
        BazelRuleLabel name, Set<BazelFileLabel> inputLabels, Set<BazelRuleLabel> inputRules) {
      return new AutoValue_DependencyGraph_Rule(
          name, ImmutableSet.copyOf(inputLabels), ImmutableSet.copyOf(inputRules));
    }
  }
}
