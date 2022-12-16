package com.stripe.build.dependencyanalyzer.bazel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CostGraphMain {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Please pass proto path to program");
      System.exit(1);
    }

    System.out.println("Parsing proto...");
    var path = Path.of(args[0]);
    var dependencies = DependencyGraph.fromProtoInputStream(Files.newInputStream(path));
    var graph = CostGraph.from(dependencies);
    graph.printSummary();
  }

  private CostGraphMain() {}
}
