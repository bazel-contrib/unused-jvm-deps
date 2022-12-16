package com.stripe.build.dependencyanalyzer.plugin;

import com.google.common.base.Splitter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.lang3.StringUtils;

public class TestCompiler {

  private Optional<String> plugin = Optional.empty();
  private final List<JavaFileObject> sourceFiles = new ArrayList<>();
  private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
  private final StandardJavaFileManager javaFileManager =
      compiler.getStandardFileManager(null, null, null);
  private String compilationOutput = "";
  private final SymbolsFileManager symbolsFileManager = new SymbolsFileManager(javaFileManager);
  private final Map<JavaFileObject, String> sourceFileToPackageName = new HashMap<>();
  private final Map<JavaFileObject, SymbolCollectionResult> sourceFileToSymbolsResults =
      new HashMap<>();

  /**
   * Compiles Java source files and reads the resulting symbols files.
   *
   * @return true if all files compiled successfully, false otherwise
   */
  public boolean compile() throws IOException {
    var output = new StringWriter();
    JavaCompiler.CompilationTask task =
        compiler.getTask(output, javaFileManager, null, getArguments(), null, sourceFiles);
    boolean result = task.call();
    compilationOutput = output.toString();
    if (result) {
      for (JavaFileObject sourceFile : sourceFiles) {
        sourceFileToSymbolsResults.put(
            sourceFile,
            symbolsFileManager.readResultsFromSymbolsFile(
                sourceFile.getName(),
                Objects.requireNonNull(sourceFileToPackageName.get(sourceFile))));
      }
    }
    javaFileManager.close();
    return result;
  }

  public TestCompiler setPlugin(String pluginName) {
    plugin = Optional.of(pluginName);
    return this;
  }

  public TestCompiler addSourceFiles(JavaFileObject... files) throws IOException {
    for (JavaFileObject file : files) {
      sourceFiles.add(file);
      sourceFileToPackageName.put(file, getPackageName(file));
    }
    return this;
  }

  public SymbolCollectionResult getSymbolCollectionResult(JavaFileObject file) {
    return Objects.requireNonNull(
        sourceFileToSymbolsResults.get(file),
        String.format("Could not find *-symbols.json file for %s", file.getName()));
  }

  public String getCompilationOutput() {
    return compilationOutput;
  }

  private List<String> getArguments() {
    List<String> arguments = new ArrayList<>();
    arguments.add("-classpath");
    arguments.add(System.getProperty("java.class.path"));
    plugin.ifPresent(s -> arguments.add(String.format("-Xplugin:%s", s)));
    return arguments;
  }

  public static String getPackageName(JavaFileObject file) throws IOException {
    BufferedReader reader = new BufferedReader(file.openReader(false));
    List<String> firstLineTokens = Splitter.on(" ").splitToList(reader.readLine());
    if (firstLineTokens.size() != 2) {
      throw new IllegalArgumentException("Malformed package declaration in " + file.getName());
    }
    return StringUtils.chop(firstLineTokens.get(1));
  }
}
