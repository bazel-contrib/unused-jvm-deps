package com.stripe.build.dependencyanalyzer.plugin;

import com.google.auto.service.AutoService;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import javax.tools.JavaFileManager;

/**
 * Compiler plugin that scans Java source code to find the exported and imported symbols in every
 * compilation unit.
 */
@AutoService(Plugin.class)
public class StripeDependencyAnalyzerPlugin implements Plugin {

  public static final String NAME = "StripeDependencyAnalyzerPlugin";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void init(JavacTask task, String... args) {
    /**
     * Bazel will pass the label of the target that's source files are being compiled as an argument
     * to the compiler plugin, but we won't pass that argument in testing, so it's optional.
     */
    String bazelTargetLabel = args.length >= 1 ? args[0] : "";

    Context context = ((BasicJavacTask) task).getContext();
    SymbolsFileManager fileManager = new SymbolsFileManager(context.get(JavaFileManager.class));
    Trees trees = Trees.instance(task);
    task.addTaskListener(
        new TaskListener() {
          @Override
          public void finished(TaskEvent e) {
            if (e.getKind() == Kind.ANALYZE) {
              SymbolCollector collector = new SymbolCollector();
              new ExportedSymbolScanner(collector).scan(e.getCompilationUnit(), null);
              new ImportedSymbolScanner(collector).scan(e.getCompilationUnit(), null);

              SymbolCollectionResult result =
                  SymbolCollectionResult.create(
                      e.getCompilationUnit().getSourceFile().getName(),
                      SymbolCollector.getFullyQualifiedName(
                              ASTHelpers.getSymbol(e.getCompilationUnit().getPackage()))
                          .toString(),
                      bazelTargetLabel,
                      collector.getSortedExportedSymbolStrings(),
                      collector.getSortedImportedSymbolStrings());
              writeSymbolsToFile(result, e.getCompilationUnit());
            }
          }

          private void writeSymbolsToFile(
              SymbolCollectionResult result, CompilationUnitTree compilationUnit) {
            try {
              fileManager.writeResultsToSymbolsFile(result);
            } catch (IOException ex) {
              trees.printMessage(
                  javax.tools.Diagnostic.Kind.ERROR,
                  "Failed to write to symbols file",
                  compilationUnit,
                  compilationUnit);
            }
          }
        });
  }
}
