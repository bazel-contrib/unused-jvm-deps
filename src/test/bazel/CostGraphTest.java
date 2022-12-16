package com.stripe.build.dependencyanalyzer.bazel;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CostGraphTest {
  @Test
  public void canParseSimpleGraph1() throws Exception {
    withStub(
        stub -> {
          // Given a dependency graph like:
          //
          //  (fruitsalad/grape) --> (fruitsalad/fruit)
          var fruit = stub.addBuildRule("//fruitsalad/fruit", ":local_dep");
          var grape = stub.addBuildRule("//fruitsalad/grape", "//fruitsalad/fruit");

          // When computing the cost graph:
          var costGraph = stub.buildCostGraph(fruit, grape);

          // It has the correct nodes:
          var fruitNode = costGraph.get(BazelRuleLabel.of("//fruitsalad/fruit"));
          var grapeNode = costGraph.get(BazelRuleLabel.of("//fruitsalad/grape"));
          assertThat(fruitNode.isPresent()).isTrue();
          assertThat(grapeNode.isPresent()).isTrue();

          // The graph should now have:
          // - //fruitsalad/fruit
          // - //fruitsalad/fruit:local_dep
          // - //fruitsalad/grape
          assertThat(costGraph.size()).isEqualTo(3);

          // It has the right amount of dependencies for each node:
          assertThat(costGraph.cost(BazelRuleLabel.of("//fruitsalad/grape"))).isEqualTo(1);
          assertThat(costGraph.cost(BazelRuleLabel.of("//fruitsalad/fruit"))).isEqualTo(2);
        });
  }

  @Test
  public void canParseSimpleGraph2() throws Exception {
    withStub(
        stub -> {
          // Given a dependency graph with a transitive dependency:
          //
          // (fruitsalad/grape) --> (fruitsalad/botanical-berry) --> (fruitsalad/fruit)
          var fruit = stub.addBuildRule("//fruitsalad/fruit");
          var berry = stub.addBuildRule("//fruitsalad/botanical-berry", "//fruitsalad/fruit");
          var grape = stub.addBuildRule("//fruitsalad/grape", "//fruitsalad/botanical-berry");

          // When computing the cost graph:
          var costGraph = stub.buildCostGraph(berry, fruit, grape);

          // It has the correct nodes:
          var fruitNode = costGraph.get(BazelRuleLabel.of("//fruitsalad/fruit"));
          var grapeNode = costGraph.get(BazelRuleLabel.of("//fruitsalad/grape"));
          var berryNode = costGraph.get(BazelRuleLabel.of("//fruitsalad/botanical-berry"));
          assertThat(fruitNode.isPresent()).isTrue();
          assertThat(grapeNode.isPresent()).isTrue();
          assertThat(berryNode.isPresent()).isTrue();

          // It has the right amount of nodes:
          assertThat(costGraph.size()).isEqualTo(3);

          // It has the right amount of dependencies for each node:
          assertThat(costGraph.cost(BazelRuleLabel.of("//fruitsalad/grape"))).isEqualTo(1);
          assertThat(costGraph.cost(BazelRuleLabel.of("//fruitsalad/botanical-berry")))
              .isEqualTo(2);
          assertThat(costGraph.cost(BazelRuleLabel.of("//fruitsalad/fruit"))).isEqualTo(3);
        });
  }

  @Test
  public void canParseMinimumCutOfSingleNode() throws Exception {
    withStub(
        stub -> {
          // Given a graph with only one node:
          var a = stub.addBuildRule("//a");
          var graph = stub.buildCostGraph(a);

          // It has the right amount of nodes:
          assertThat(graph.size()).isEqualTo(1);

          // The cost of rebuilding {a} is just the node itself:
          var cost = graph.cost(Set.of(BazelRuleLabel.of("//a")));
          assertThat(cost).isEqualTo(1);
        });
  }

  @Test
  public void canParseMinimumCutOfDeps1() throws Exception {
    withStub(
        stub -> {
          // Given a graph like:
          //
          // a --> b --> c
          //       ^
          //       |
          // d ----+---> e
          var a = stub.addBuildRule("//a", "//b");
          var b = stub.addBuildRule("//b", "//c");
          var c = stub.addBuildRule("//c");
          var d = stub.addBuildRule("//d", "//b", "//e");
          var e = stub.addBuildRule("//e");

          var graph = stub.buildCostGraph(a, b, c, d, e);

          // It has the right amount of nodes:
          assertThat(graph.size()).isEqualTo(5);

          // The cost of rebuilding {c, e} is equal to the size of the graph:
          var cost = graph.cost(Set.of(BazelRuleLabel.of("//c"), BazelRuleLabel.of("//e")));
          assertThat(cost).isEqualTo(5);
        });
  }

  @Test
  public void canParseMinimumCutOfDeps2() throws Exception {
    withStub(
        stub -> {
          // Given a graph like:
          //
          // a --> b --> c
          //       ^
          //       |
          // d ----+---> e
          //
          var a = stub.addBuildRule("//a", "//b");
          var b = stub.addBuildRule("//b", "//c");
          var c = stub.addBuildRule("//c");
          var d = stub.addBuildRule("//d", "//b", "//e");
          var e = stub.addBuildRule("//e");

          var graph = stub.buildCostGraph(a, b, c, d, e);

          // It has the right amount of nodes:
          assertThat(graph.size()).isEqualTo(5);

          // The cost of rebuilding {e} is sizeof {d} + 1:
          var cost = graph.cost(Set.of(BazelRuleLabel.of("//e")));
          assertThat(cost).isEqualTo(2);
        });
  }

  @Test
  public void canParseMinimumCutOfDeps3() throws Exception {
    withStub(
        stub -> {
          // Given a graph like:
          //
          // a --> b --> c
          //       ^
          //       |
          // d ----+---> e
          //
          var a = stub.addBuildRule("//a", "//b");
          var b = stub.addBuildRule("//b", "//c");
          var c = stub.addBuildRule("//c");
          var d = stub.addBuildRule("//d", "//b", "//e");
          var e = stub.addBuildRule("//e");

          var graph = stub.buildCostGraph(a, b, c, d, e);

          // It has the right amount of nodes:
          assertThat(graph.size()).isEqualTo(5);

          // Given an unknown file in the //e target, we should still be able to calculate the cost:
          var cost = graph.cost(BazelFileLabel.of("//e:Banana.java").get());
          assertThat(cost).isEqualTo(2);
        });
  }

  @Test
  public void returnsMaxCostForNonExistentTarget() throws Exception {
    withStub(
        stub -> {
          // Given a graph like:
          //
          // a --> b --> c
          var a = stub.addBuildRule("//a", "//b");
          var b = stub.addBuildRule("//b", "//c");
          var c = stub.addBuildRule("//c");

          var graph = stub.buildCostGraph(a, b, c);

          assertThat(graph.size()).isEqualTo(3);

          var cost = graph.cost(Set.of(BazelRuleLabel.of("//foo:bar.java")));
          assertThat(cost).isEqualTo(Integer.MAX_VALUE);
        });
  }

  @Test
  public void returnsMaxCostForEmptyGraph() throws Exception {
    withStub(
        stub -> {
          var graph = stub.buildCostGraph();

          assertThat(graph.size()).isEqualTo(0);

          var cost = graph.cost(BazelFileLabel.of("//foo:bar.java").get());
          assertThat(cost).isEqualTo(Integer.MAX_VALUE);
        });
  }

  private static void withStub(ExceptionalConsumer<Stub> fun) throws Exception {
    var stub = new Stub();
    fun.apply(stub);
  }

  private static class Stub {
    public CostGraph buildCostGraph(Build.Target... targets) {
      var result = Build.QueryResult.newBuilder().addAllTarget(Arrays.asList(targets));
      var deps = DependencyGraph.fromBuildResult(result.build(), "/Users/me/myrepo");
      return CostGraph.from(deps);
    }

    /** Create a bazel build file in `dir` and with the supplied `deps` */
    public Build.Target addBuildRule(String name, String... deps) throws IOException {
      var rule =
          Build.Rule.newBuilder()
              .setLocation("/Users/me/myrepo" + name.substring(1) + "/BUILD.bazel:1:1")
              .setRuleClass("java_lib")
              .addAllRuleInput(Arrays.asList(deps))
              .setName(name);

      return Build.Target.newBuilder()
          .setRule(rule)
          .setType(Build.Target.Discriminator.RULE)
          .build();
    }
  }
}
