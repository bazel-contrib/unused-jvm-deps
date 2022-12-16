package com.stripe.build.dependencyanalyzer.plugin;

import static com.stripe.build.dependencyanalyzer.plugin.TestCompiler.getPackageName;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StripeDependencyAnalyzerPluginTest {

  /**
   * Any class that does not extend a superclass (and thus extends java.lang.Object), will import
   * these symbols.
   */
  private static final ImmutableSet<String> OBJECT_IMPORTS =
      ImmutableSet.of("java.lang.Object", "java.lang.Object.<init>");

  private TestCompiler testCompiler;

  @Before
  public void setup() {
    testCompiler = new TestCompiler().setPlugin(StripeDependencyAnalyzerPlugin.NAME);
  }

  /**
   * Tests that all non-private members are collected as exported symbols and that private members
   * are not.
   */
  @Test
  public void testExportsMembersAccessModifiers() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ExportsMembersAccessModifiers.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedExportedSymbols =
        qualifySymbols(
            getPackageName(source),
            Set.of(
                "ExportsMembersAccessModifiers",
                "ExportsMembersAccessModifiers.<init>",
                "ExportsMembersAccessModifiers.packagePrivateField",
                "ExportsMembersAccessModifiers.packagePrivateMethod",
                "ExportsMembersAccessModifiers.protectedField",
                "ExportsMembersAccessModifiers.protectedMethod",
                "ExportsMembersAccessModifiers.publicField",
                "ExportsMembersAccessModifiers.publicMethod"));
    Assert.assertEquals(expectedExportedSymbols, result.getExportedSymbols());
  }

  /** Tests that static members and nested classes are collected as exported symbols. */
  @Test
  public void testExportsStatics() throws IOException {
    JavaFileObject source = JavaFileObjects.forResource("testResources/plugin/ExportsStatics.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedExportedSymbols =
        qualifySymbols(
            getPackageName(source),
            Set.of(
                "ExportsStatics",
                "ExportsStatics.<init>",
                "ExportsStatics.CONSTANT",
                "ExportsStatics.staticField",
                "ExportsStatics.staticMethod",
                "ExportsStatics.NestedStaticClass",
                "ExportsStatics.NestedStaticClass.<init>",
                "ExportsStatics.NestedStaticClass.nestedField",
                "ExportsStatics.NestedStaticClass.nestedMethod"));
    Assert.assertEquals(expectedExportedSymbols, result.getExportedSymbols());
  }

  /**
   * Tests that non-private nested classes and their members are also collected as exported symbols.
   */
  @Test
  public void testExportsNestedClass() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ExportsNestedClass.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedExportedSymbols =
        qualifySymbols(
            getPackageName(source),
            Set.of(
                "ExportsNestedClass",
                "ExportsNestedClass.<init>",
                "ExportsNestedClass.NestedClass",
                "ExportsNestedClass.NestedClass.<init>",
                "ExportsNestedClass.NestedClass.nestedMember",
                "ExportsNestedClass.NestedClass.DoubleNestedClass",
                "ExportsNestedClass.NestedClass.DoubleNestedClass.<init>",
                "ExportsNestedClass.NestedClass.DoubleNestedClass.doubleNestedMember"));
    Assert.assertEquals(expectedExportedSymbols, result.getExportedSymbols());
  }

  /**
   * Tests that non-private nested classes within private nested classes are not collected as
   * exported symbols.
   */
  @Test
  public void testExportsNestedPrivateClass() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ExportsNestedPrivateClass.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedExportedSymbols =
        qualifySymbols(
            getPackageName(source),
            Set.of("ExportsNestedPrivateClass", "ExportsNestedPrivateClass.<init>"));
    Assert.assertEquals(expectedExportedSymbols, result.getExportedSymbols());
  }

  /** Tests that type parameter constraints are collected as imported symbols. */
  @Test
  public void testImportsConstraints() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ImportsConstraints.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .add(
                "java.time.Duration", // type parameter constraint
                "java.time.Clock", // wildcard constraint
                "java.util.List",
                // interfaces implemented / classes extended by above
                "java.time.temporal.TemporalAmount",
                "java.io.Serializable",
                "java.lang.Comparable",
                "java.util.Collection")
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that implemented interfaces, superclasses, and referenced superclass fields are collected
   * as imported symbols.
   */
  @Test
  public void testImportsClassExtension() throws IOException {
    JavaFileObject classExtensionSource =
        JavaFileObjects.forResource("testResources/plugin/ImportsClassExtension.java");
    JavaFileObject superclassSource =
        JavaFileObjects.forResource("testResources/plugin/Superclass.java");
    JavaFileObject interfaceSource =
        JavaFileObjects.forResource("testResources/plugin/SimpleInterface.java");
    compileSourceFiles(classExtensionSource, superclassSource, interfaceSource);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(classExtensionSource);

    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .addAll(
                qualifySymbols(
                    getPackageName(superclassSource),
                    Set.of(
                        "Superclass",
                        "Superclass.<init>",
                        "Superclass.superField",
                        "Superclass.superStaticMethod")))
            .addAll(qualifySymbols(getPackageName(interfaceSource), Set.of("SimpleInterface")))
            .add(
                "java.lang.Override",
                "java.lang.Number",
                "java.lang.String",
                "java.lang.annotation.Annotation",
                // interfaces implemented / classes extended by String
                "java.io.Serializable",
                "java.lang.CharSequence",
                "java.lang.Comparable",
                "java.lang.Object")
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /** Tests that annotations are collected as imported symbols. */
  @Test
  public void testImportsAnnotations() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ImportsAnnotations.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .add(
                "java.beans.JavaBean",
                "javax.annotation.Nullable",
                "javax.validation.constraints.Positive",
                "java.lang.annotation.Annotation",
                "java.lang.String",
                // interfaces implemented / classes extended by String
                "java.io.Serializable",
                "java.lang.CharSequence",
                "java.lang.Comparable")
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that types of fields and return types and parameters of methods are collected as imported
   * symbols.
   */
  @Test
  public void testImportsMembersDefinitions() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ImportsMembersDefinitions.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .add(
                "java.time.Clock",
                "java.time.Duration",
                "java.time.Year",
                // interfaces implemented / classes extended by above
                "java.time.temporal.Temporal",
                "java.time.temporal.TemporalAdjuster",
                "java.time.temporal.TemporalAmount",
                "java.io.Serializable",
                "java.lang.Comparable")
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that symbols accessed via their fully qualified names without import statements are
   * collected as imported symbols.
   */
  @Test
  public void testImportsFullyQualifiedNames() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ImportsFullyQualifiedNames.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder().add("java.time.Clock").addAll(OBJECT_IMPORTS).build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /** Tests that imports of static members and nested classes are collected as imported symbols. */
  @Test
  public void testImportsStatics() throws IOException {
    JavaFileObject importsStaticsSource =
        JavaFileObjects.forResource("testResources/plugin/ImportsStatics.java");
    JavaFileObject exportsStaticsSource =
        JavaFileObjects.forResource("testResources/plugin/ExportsStatics.java");
    compileSourceFiles(importsStaticsSource, exportsStaticsSource);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(importsStaticsSource);

    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .addAll(
                qualifySymbols(
                    getPackageName(exportsStaticsSource),
                    Set.of(
                        "ExportsStatics",
                        "ExportsStatics.CONSTANT",
                        "ExportsStatics.staticField",
                        "ExportsStatics.staticMethod")))
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that references to other classes that are defined in the same file are not collected as
   * imported symbols.
   */
  @Test
  public void testImportsIgnoreDefinedInFile() throws IOException {
    JavaFileObject source =
        JavaFileObjects.forResource("testResources/plugin/ImportsIgnoreDefinedInFile.java");
    compileSourceFiles(source);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(source);
    Set<String> expectedImportedSymbols = OBJECT_IMPORTS;
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that members in the middle of a member select chain (eg: memberA in
   * ClassA.memberA.memberC) as well as their types are collected as imported symbols.
   */
  @Test
  public void testImportsMemberSelects() throws IOException {
    JavaFileObject importsMemberSelectsSource =
        JavaFileObjects.forResource("testResources/plugin/ImportsMemberSelects.java");
    JavaFileObject exportsStaticsSource =
        JavaFileObjects.forResource("testResources/plugin/ExportsStatics.java");
    compileSourceFiles(importsMemberSelectsSource, exportsStaticsSource);
    SymbolCollectionResult result =
        testCompiler.getSymbolCollectionResult(importsMemberSelectsSource);

    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .addAll(
                qualifySymbols(
                    getPackageName(exportsStaticsSource),
                    Set.of(
                        "ExportsStatics",
                        "ExportsStatics.NestedStaticClass",
                        "ExportsStatics.NestedStaticClass.nestedMethod")))
            .add(
                "java.lang.String",
                "java.lang.String.length",
                // interfaces implemented by String
                "java.io.Serializable",
                "java.lang.CharSequence",
                "java.lang.Comparable")
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that when we reference as an import a ClassType type that we also reference the imported
   * type class's enclosing class, supertype, and implemented interfaces.
   */
  @Test
  public void testImportsConstrainedType() throws IOException {
    JavaFileObject importsSource =
        JavaFileObjects.forResource("testResources/plugin/ImportsConstrainedType.java");
    JavaFileObject exportsSource =
        JavaFileObjects.forResource("testResources/plugin/ConstrainedType.java");
    compileSourceFiles(importsSource, exportsSource);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(importsSource);

    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .addAll(
                qualifySymbols(
                    getPackageName(exportsSource),
                    Set.of(
                        "ConstrainedType",
                        "ConstrainedType.getNested",
                        "ConstrainedType.NestedClass",
                        "ConstrainedType.MyInterface",
                        "ConstrainedType.BaseNestedClass")))
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that when a method invocation is referenced, the types of the method arguments are
   * collected as imported.
   */
  @Test
  public void testImportsInvokedMethodArgumentType() throws IOException {
    JavaFileObject importsSource =
        JavaFileObjects.forResource("testResources/plugin/ImportsInvokedMethodArgumentType.java");
    JavaFileObject exportsSource =
        JavaFileObjects.forResource("testResources/plugin/ConstrainedType.java");
    compileSourceFiles(importsSource, exportsSource);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(importsSource);

    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .addAll(
                qualifySymbols(
                    getPackageName(exportsSource),
                    Set.of(
                        "ConstrainedType",
                        "ConstrainedType.useNested",
                        "ConstrainedType.NestedClass",
                        "ConstrainedType.BaseNestedClass",
                        "ConstrainedType.MyInterface")))
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  /**
   * Tests that when a method invocation is referenced, the types of all overloaded methods are also
   * referenced as imports.
   */
  @Test
  public void testImportsInvokedOverloadedMethodTypes() throws IOException {
    JavaFileObject importsSource =
        JavaFileObjects.forResource("testResources/plugin/ImportsOverloadedMethod.java");
    JavaFileObject exportsSource =
        JavaFileObjects.forResource("testResources/plugin/ExportsOverloadedMethod.java");
    compileSourceFiles(importsSource, exportsSource);
    SymbolCollectionResult result = testCompiler.getSymbolCollectionResult(importsSource);

    Set<String> expectedImportedSymbols =
        ImmutableSet.<String>builder()
            .addAll(
                qualifySymbols(
                    getPackageName(exportsSource),
                    Set.of(
                        "ExportsOverloadedMethod",
                        "ExportsOverloadedMethod.overloaded",
                        "ExportsOverloadedMethod.TypeA",
                        "ExportsOverloadedMethod.TypeB")))
            .addAll(OBJECT_IMPORTS)
            .build();
    Assert.assertEquals(expectedImportedSymbols, result.getImportedSymbols());
  }

  private void compileSourceFiles(JavaFileObject... sourceFiles) throws IOException {
    boolean compilationResult = testCompiler.addSourceFiles(sourceFiles).compile();
    Assert.assertTrue(
        "Compilation of test sources failed, output below:\n" + testCompiler.getCompilationOutput(),
        compilationResult);
  }

  private Set<String> qualifySymbols(String packageName, Collection<String> symbols) {
    return symbols.stream()
        .map(s -> String.format("%s.%s", packageName, s))
        .collect(Collectors.toSet());
  }
}
