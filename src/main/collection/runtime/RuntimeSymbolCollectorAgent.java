package com.stripe.build.dependencyanalyzer.collection.runtime;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

public class RuntimeSymbolCollectorAgent {
  private final Instrumentation instrumentation;
  private final Path outputFile;

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    new RuntimeSymbolCollectorAgent(instrumentation, Path.of(agentArgs).toAbsolutePath()).start();
  }

  public RuntimeSymbolCollectorAgent(Instrumentation instrumentation, Path outputFile) {
    this.instrumentation = instrumentation;
    this.outputFile = outputFile;
    System.err.println(
        "["
            + RuntimeSymbolCollectorAgent.class.getSimpleName()
            + "] Created; will write to "
            + outputFile);
  }

  private void start() {
    System.err.println("[" + RuntimeSymbolCollectorAgent.class.getSimpleName() + "] Starting up");
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                this::handleShutdown,
                RuntimeSymbolCollectorAgent.class.getSimpleName() + " JVM shutdown hook"));
  }

  private void handleShutdown() {
    try (var writer = new GsonBuilder().setPrettyPrinting().create().newJsonWriter(newBufferedWriter(outputFile, UTF_8))) {
      writer.beginObject();
      writer.name("loadedClasses");
      writer.beginArray();
      var sortedClasses =
          Arrays.stream(instrumentation.getAllLoadedClasses())
                  .filter(not(Class::isArray))
              .sorted(comparing(Class::getName))
              .collect(toList());
      for (var c : sortedClasses) {
        writer.value(c.getName());
      }
      writer.endArray();
      writer.endObject();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
