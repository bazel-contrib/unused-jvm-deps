package com.stripe.build.dependencyanalyzer.plugin;

import com.google.common.io.Files;
import com.google.gson.JsonStreamParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

public class SymbolsFileManager {
  private final JavaFileManager javaFileManager;

  public SymbolsFileManager(JavaFileManager javaFileManager) {
    this.javaFileManager = javaFileManager;
  }

  public URI writeResultsToSymbolsFile(SymbolCollectionResult symbolsResult) throws IOException {
    FileObject outputFile =
        getSymbolsFile(symbolsResult.getSourceFileName(), symbolsResult.getPackageName());
    Writer writer = outputFile.openWriter();
    writer.append(symbolsResult.toJsonString());
    writer.close();
    javaFileManager.flush();
    return outputFile.toUri();
  }

  public SymbolCollectionResult readResultsFromSymbolsFile(
      String sourceFileName, String packageName) throws IOException {
    return parseSymbolsFile(getSymbolsFile(sourceFileName, packageName));
  }

  private FileObject getSymbolsFile(String sourceFileName, String packageName) throws IOException {
    String symbolsFileName = Files.getNameWithoutExtension(sourceFileName) + "-symbols.json";
    return javaFileManager.getFileForOutput(
        StandardLocation.CLASS_OUTPUT, packageName, symbolsFileName, null);
  }

  private SymbolCollectionResult parseSymbolsFile(FileObject symbolsFile) throws IOException {
    Reader reader = symbolsFile.openReader(false);
    var parser = new JsonStreamParser(reader);
    return SymbolCollectionResult.fromJsonObject(parser.next().getAsJsonObject());
  }
}
