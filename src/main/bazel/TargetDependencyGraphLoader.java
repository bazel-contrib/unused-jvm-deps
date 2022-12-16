package com.stripe.build.dependencyanalyzer.bazel;

import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.QueryResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class TargetDependencyGraphLoader {
  /**
   * Limit the deps the query returns to these kinds. We filter at the Bazel level (rather than our
   * own code) because it's quicker empirically.
   */
  private static final Set<String> DEP_KINDS =
      Set.of("java_library", "java_binary", "java_import", "jvm_import");

  /** Should strip the dependency universe suffixes from third-party targets. */
  private static final Set<String> DEPENDENCY_UNIVERSES = Set.of("current", "next", "spark31");

  private static final String DEPENDENCY_UNIVERSE_REGEX =
      String.format(":(%s)_", String.join("|", DEPENDENCY_UNIVERSES));

  private final Path bazelWorkspace;

  /**
   * Store the indices of attributes in the attribute list for each rule class. For example, that in
   * 'java_library' targets, the 'deps' attribute is at index 15.
   */
  private final Map<String, Map<String, Integer>> ruleClassToAttributeNameToIndex = new HashMap<>();

  private TargetDependencyGraphLoader(Path workingDirectory) {
    this.bazelWorkspace = workingDirectory;
  }

  /**
   * Queries Bazel to retrieve the dependency graph of a target, parsing it into a {@link
   * com.stripe.build.dependencyanalyzer.bazel.DependencyGraph}.
   *
   * @param target the root Bazel target to query
   * @param bazelWorkspace Bazel workspace root directory (should contain a WORKSPACE file)
   * @return a {@link TargetDependencyGraphLoadResult} object containing the result
   */
  public static TargetDependencyGraphLoadResult load(String target, Path bazelWorkspace)
      throws IOException {
    return new TargetDependencyGraphLoader(bazelWorkspace).load(target);
  }

  private TargetDependencyGraphLoadResult load(String target) throws IOException {
    Instant startTime = Clock.systemUTC().instant();

    String bazelQuery = createBazelQueryString(target);
    var queryProcBuilder =
        new ProcessBuilder()
            .directory(new File(bazelWorkspace.toString()))
            .command("bazel", "query", "--output=proto", "--keep_going", bazelQuery);
    Process queryProc = queryProcBuilder.start();

    QueryResult queryResult =
        QueryResult.parseFrom(IOUtils.toByteArray(queryProc.getInputStream()));
    Map<BazelRuleLabel, ParsedBazelTarget> parsedTargets = parseQueryResult(queryResult);
    String bazelErrors =
        IOUtils.toString(queryProc.getErrorStream(), StandardCharsets.UTF_8).trim();

    Instant endTime = Clock.systemUTC().instant();
    return TargetDependencyGraphLoadResult.create(
        target, bazelQuery, bazelErrors, parsedTargets, Duration.between(startTime, endTime));
  }

  private Map<BazelRuleLabel, ParsedBazelTarget> parseQueryResult(QueryResult result) {
    Map<BazelRuleLabel, Build.Rule> labelToRule = new HashMap<>();
    for (Build.Target target : result.getTargetList()) {
      if (target.hasRule()) {
        Build.Rule targetRule = target.getRule();
        labelToRule.put(
            BazelRuleLabel.of(removeDependencyUniverseFromLabel(targetRule.getName())), targetRule);
      }
    }

    Map<BazelRuleLabel, ParsedBazelTarget> labelToParsedTarget = new HashMap<>();
    for (var entry : labelToRule.entrySet()) {
      var label = entry.getKey();
      var rule = entry.getValue();
      List<BazelRuleLabel> deps = getRuleLabelsForAttribute(rule, "deps", labelToRule.keySet());
      List<BazelRuleLabel> exports =
          getRuleLabelsForAttribute(rule, "exports", labelToRule.keySet());
      labelToParsedTarget.put(label, ParsedBazelTarget.create(label, deps, exports));
    }
    return labelToParsedTarget;
  }

  private List<BazelRuleLabel> getRuleLabelsForAttribute(
      Build.Rule rule, String attributeName, Set<BazelRuleLabel> validRuleLabels) {
    Optional<Directory> parsedDirectory = Directory.parse(rule.getLocation(), "");
    if (parsedDirectory.isEmpty()) {
      return List.of();
    }
    Directory directory = parsedDirectory.get();

    return getValueListForAttribute(rule, attributeName).stream()
        .map(v -> BazelRuleLabel.fromInputLabel(directory, removeDependencyUniverseFromLabel(v)))
        .flatMap(Optional::stream)
        .filter(validRuleLabels::contains)
        .collect(Collectors.toList());
  }

  private List<String> getValueListForAttribute(Build.Rule rule, String attributeName) {
    int index = getAttributeIndex(rule, attributeName);
    if (index < 0) {
      return new ArrayList<>();
    }
    Build.Attribute attribute = rule.getAttributeList().get(index);
    return attribute.getStringListValueList();
  }

  private int getAttributeIndex(Build.Rule rule, String attributeName) {
    return ruleClassToAttributeNameToIndex
        .computeIfAbsent(rule.getRuleClass(), k -> new HashMap<>())
        .computeIfAbsent(attributeName, k -> findAttributeIndex(rule, attributeName));
  }

  private int findAttributeIndex(Build.Rule rule, String attributeName) {
    List<Build.Attribute> attributeList = rule.getAttributeList();
    for (int i = 0; i < attributeList.size(); i++) {
      if (StringUtils.equals(attributeList.get(i).getName(), attributeName)) {
        return i;
      }
    }
    return -1;
  }

  private String removeDependencyUniverseFromLabel(String targetLabel) {
    return targetLabel.replaceAll(DEPENDENCY_UNIVERSE_REGEX, ":");
  }

  private String createBazelQueryString(String target) {
    return kind(DEP_KINDS, deps(target));
  }

  private String deps(String expression) {
    return String.format("deps(%s)", expression);
  }

  private String kind(Set<String> kinds, String expression) {
    return String.format("kind(\"%s\", %s)", String.join("|", kinds), expression);
  }
}
