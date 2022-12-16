package com.stripe.build.dependencyanalyzer.plugin;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.Modifier;

/**
 * Scanner that will collect every symbol that a Java source file (compilation unit) exports. We say
 * that a symbol is an 'export' of a source file if it is a non-private class or a non-private
 * member (static or instance) of any such non-private class defined in that file. This does not
 * include non-private members or subclasses of private classes, as they are not accessible from
 * outside this file. This also does not include inherited members from superclasses defined in
 * another file, as those symbols are not exports of the current source file.
 */
public class ExportedSymbolScanner extends TreePathScanner<Void, Void> {

  private final SymbolCollector collector;

  public ExportedSymbolScanner(SymbolCollector collector) {
    this.collector = collector;
  }

  @Override
  public Void visitClass(ClassTree node, Void unused) {
    Symbol.ClassSymbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null && !isPrivate(symbol)) {
      collector.addExportedSymbol(symbol);
      return super.visitClass(node, unused);
    } else {
      return null; // nothing inside a private class is an export, so don't recur
    }
  }

  @Override
  public Void visitMethod(MethodTree node, Void unused) {
    Symbol.MethodSymbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null && !isPrivate(symbol)) {
      collector.addExportedSymbol(symbol);
    }
    return null; // nothing inside a method is an export, so don't recur
  }

  @Override
  public Void visitVariable(VariableTree node, Void unused) {
    Symbol.VarSymbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null && !isPrivate(symbol)) {
      collector.addExportedSymbol(symbol);
    }
    return null; // nothing inside a variable is an export, so don't recur
  }

  private boolean isPrivate(Symbol symbol) {
    return symbol.getModifiers().contains(Modifier.PRIVATE);
  }
}
