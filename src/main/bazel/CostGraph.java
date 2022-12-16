package com.stripe.build.dependencyanalyzer.bazel;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This dependency graph inverts a parsed dependency graph in order to be able to calculate the
 * number of dependencies on each node.
 */
public class CostGraph {
  private static final Logger log = LoggerFactory.getLogger(CostGraph.class);

  private final Map<BazelRuleLabel, InvertedBazelRule> rules = new HashMap<>();
  private final Map<BazelRuleLabel, Integer> costs = new HashMap<>();
  private final Map<Directory, Set<BazelRuleLabel>> directoryToRules;
  private final Map<BazelFileLabel, Set<BazelRuleLabel>> fileToRules;

  protected CostGraph(
      Map<BazelFileLabel, Set<BazelRuleLabel>> fileToRules,
      Map<Directory, Set<BazelRuleLabel>> directoryToRules) {
    this.fileToRules = fileToRules;
    this.directoryToRules = directoryToRules;
  }

  /** Invert the given dependency graph into a cost graph */
  public static CostGraph from(DependencyGraph graph) {
    var inverted = new CostGraph(graph.labelToName, graph.locationToRule);
    inverted.populateTree(graph);
    inverted.calculateCosts();
    return inverted;
  }

  // Populate the tree and turn dependencies into dependents:
  private void populateTree(DependencyGraph graph) {
    graph.nameToRule.forEach(
        (name, rule) -> {
          // Create an inverted rule:
          var newRule = new InvertedBazelRule(name);
          rules.putIfAbsent(name, newRule);
          var inverted = rules.getOrDefault(name, newRule);

          for (var dep : rule.inputRules()) {
            var newDep = new InvertedBazelRule(dep);
            rules.putIfAbsent(dep, newDep);
            rules.getOrDefault(dep, newDep).addDirectDependent(inverted);
          }
        });
  }

  // Pre-calculate the cost on a per-rule basis:
  private void calculateCosts() {
    rules.forEach(
        (name, rule) -> {
          var cost = cost(name);
          log.info("Cost for {} was {}", name, cost);
          costs.put(name, cost);
        });
  }

  /** Prints a summary of the cost graph */
  public void printSummary() {
    costs.entrySet().stream()
        .sorted(Map.Entry.<BazelRuleLabel, Integer>comparingByValue().reversed())
        .forEach(e -> log.info("Target cost {}: {}", e.getKey().value(), e.getValue()));
  }

  /** Get the rule from a given label */
  public Optional<InvertedBazelRule> get(BazelRuleLabel label) {
    return Optional.ofNullable(rules.get(label));
  }

  /** Get the set of rules from a given directory */
  public Optional<Set<BazelRuleLabel>> get(Directory dir) {
    return Optional.ofNullable(directoryToRules.get(dir));
  }

  /**
   * Get the cost associated with a changed file, or if that file label does not exist, fall back to
   * directory
   */
  public int cost(BazelFileLabel... labels) {
    var rules = new HashSet<BazelRuleLabel>();
    for (var fileLabel : labels) {
      // If we already know where the file label belongs, get those rules exactly, otherwise fall
      // back to looking at the directory:
      Optional.ofNullable(fileToRules.get(fileLabel))
          .orElseGet(() -> directoryToRules.getOrDefault(fileLabel.toDirectory(), Set.of()))
          .forEach(rules::add);
    }
    return cost(rules);
  }

  /** Get the cost of a single target */
  public int cost(BazelRuleLabel target) {
    return cost(Set.of(target));
  }

  /** Get the cost of the union of several targets */
  public int cost(Set<BazelRuleLabel> targets) {

    if (targets.isEmpty()) {
      return Integer.MAX_VALUE;
    }

    var dependencies = new HashSet<InvertedBazelRule>();
    var visitList = new ArrayDeque<InvertedBazelRule>();

    // Add all the targets to the visit list
    targets.stream()
        .flatMap(t -> this.get(t).stream())
        .flatMap(r -> r.directDependents.stream())
        .collect(Collectors.toSet())
        .forEach(visitList::add);

    if (visitList.isEmpty()) {
      // Either the targets have no deps, or they don't exist in the graph:
      if (targets.stream().allMatch(t -> get(t).isPresent())) {
        return (targets.size());
      }

      // One or more of the targets do not exist in the graph:
      return Integer.MAX_VALUE;
    }

    while (!visitList.isEmpty()) {
      var current = visitList.pop();
      if (dependencies.add(current)) {
        current.directDependents.forEach(visitList::push);
      }
    }

    return dependencies.size() + targets.size();
  }

  /** Get the number of rules in the cost graph */
  public int size() {
    return rules.size();
  }

  public static final class InvertedBazelRule {
    public final BazelRuleLabel name;
    private final Set<InvertedBazelRule> directDependents = new HashSet<>();

    private InvertedBazelRule(BazelRuleLabel name) {
      this.name = name;
    }

    private boolean addDirectDependent(InvertedBazelRule rule) {
      return this.directDependents.add(rule);
    }
  }
}
