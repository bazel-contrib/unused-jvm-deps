package com.stripe.build.dependencyanalyzer.plugin;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoValue
public abstract class SymbolCollectionResult {

  private static final String SOURCE_FILE_NAME_PROPERTY = "sourceFileName";
  private static final String PACKAGE_NAME_PROPERTY = "packageName";
  private static final String BAZEL_TARGET_LABEL_PROPERTY = "bazelTargetLabel";
  private static final String EXPORTED_SYMBOLS_PROPERTY = "exportedSymbols";
  private static final String IMPORTED_SYMBOLS_PROPERTY = "importedSymbols";

  public abstract String getSourceFileName();

  public abstract String getPackageName();

  public abstract String getBazelTargetLabel();

  public abstract ImmutableSet<String> getExportedSymbols();

  public abstract ImmutableSet<String> getImportedSymbols();

  public Set<String> getAllSymbols() {
    return Stream.concat(getExportedSymbols().stream(), getImportedSymbols().stream())
        .collect(Collectors.toSet());
  }

  public String toJsonString() {
    return new GsonBuilder().setPrettyPrinting().create().toJson(toJsonObject());
  }

  public JsonObject toJsonObject() {
    JsonObject object = new JsonObject();
    object.addProperty(SOURCE_FILE_NAME_PROPERTY, getSourceFileName());
    object.addProperty(PACKAGE_NAME_PROPERTY, getPackageName());
    object.addProperty(BAZEL_TARGET_LABEL_PROPERTY, getBazelTargetLabel());
    object.add(EXPORTED_SYMBOLS_PROPERTY, makeJsonArray(List.copyOf(getExportedSymbols())));
    object.add(IMPORTED_SYMBOLS_PROPERTY, makeJsonArray(List.copyOf(getImportedSymbols())));
    return object;
  }

  private JsonArray makeJsonArray(List<String> symbols) {
    JsonArray array = new JsonArray();
    symbols.forEach(array::add);
    return array;
  }

  public static SymbolCollectionResult create(
      String sourceFileName,
      String packageName,
      String bazelTargetLabel,
      Collection<String> exportedSymbols,
      Collection<String> importedSymbols) {
    return new AutoValue_SymbolCollectionResult(
        sourceFileName,
        packageName,
        bazelTargetLabel,
        ImmutableSet.copyOf(exportedSymbols),
        ImmutableSet.copyOf(importedSymbols));
  }

  public static SymbolCollectionResult fromJsonObject(JsonObject jsonObject) {
    Gson gson = new Gson();
    Type stringListType = new TypeToken<List<String>>() {}.getType();
    return create(
        jsonObject.get(SOURCE_FILE_NAME_PROPERTY).getAsString(),
        jsonObject.get(PACKAGE_NAME_PROPERTY).getAsString(),
        jsonObject.get(BAZEL_TARGET_LABEL_PROPERTY).getAsString(),
        gson.fromJson(jsonObject.getAsJsonArray(EXPORTED_SYMBOLS_PROPERTY), stringListType),
        gson.fromJson(jsonObject.getAsJsonArray(IMPORTED_SYMBOLS_PROPERTY), stringListType));
  }
}
