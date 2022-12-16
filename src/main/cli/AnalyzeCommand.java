package com.stripe.build.dependencyanalyzer.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "analyze",
    description =
        "Performs various Bazel dependency analyses using the dependency graph stored in the input"
            + " database",
    subcommands = {AnalyzeUnusedCommand.class, CommandLine.HelpCommand.class})
public class AnalyzeCommand {}
