package com.stripe.build.dependencyanalyzer.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "cli",
    subcommands = {
      CollectCommand.class,
      AnalyzeCommand.class,
      CommandLine.HelpCommand.class,
    },
    description = "CLI for Bazel Dependency Analyzer")
public class Main {

  public static void main(String... args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
