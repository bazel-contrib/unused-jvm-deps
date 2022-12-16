package com.stripe.build.dependencyanalyzer.plugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SymbolCollector {

  private final Set<Symbol> exportedSymbols = new HashSet<>();
  private final Set<Symbol> importedSymbols = new HashSet<>();

  public void addExportedSymbol(Symbol symbol) {
    exportedSymbols.add(symbol);
  }

  public void addImportedSymbol(Symbol symbol) {
    importedSymbols.add(symbol);
  }

  public List<String> getSortedExportedSymbolStrings() {
    return getSortedSymbolStrings(exportedSymbols);
  }

  public List<String> getSortedImportedSymbolStrings() {
    return getSortedSymbolStrings(importedSymbols);
  }

  public static Name getFullyQualifiedName(Symbol symbol) {
    // class and package symbol types already handle fully qualified name
    return (symbol instanceof Symbol.ClassSymbol || symbol instanceof Symbol.PackageSymbol)
        ? symbol.getQualifiedName()
        : getFullyQualifiedName(symbol.owner).append('.', symbol.getQualifiedName());
  }

  private List<String> getSortedSymbolStrings(Collection<Symbol> symbols) {
    return symbols.stream()
        .map(s -> getFullyQualifiedName(s).toString())
        .sorted()
        .collect(Collectors.toList());
  }
}
