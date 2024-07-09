package com.stripe.build.dependencyanalyzer.plugin;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.tools.JavaFileObject;

/**
 * Scanner that will collect every symbol that a Java source file (compilation unit) imports from
 * other source files or external libraries.
 */
public class ImportedSymbolScanner extends TreePathScanner<Void, Void> {

  private final SymbolCollector collector;
  private CompilationUnitTree currentCompilationUnit;

  public ImportedSymbolScanner(SymbolCollector collector) {
    this.collector = collector;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
    currentCompilationUnit = node;
    return super.visitCompilationUnit(node, unused);
  }

  @Override
  public Void visitPackage(PackageTree node, Void unused) {
    // we don't want to explore the PackageTree because it leads to extraneous MemberReferenceTrees
    return null;
  }

  @Override
  public Void visitImport(ImportTree node, Void unused) {
    Symbol symbol = ASTHelpers.getSymbol(node.getQualifiedIdentifier());
    if (symbol != null) {
      referenceSymbol(symbol);
    }
    return super.visitImport(node, unused);
  }

  @Override
  public Void visitClass(ClassTree node, Void unused) {
    Symbol.ClassSymbol classSymbol = ASTHelpers.getSymbol(node);
    referenceSymbol(classSymbol);
    referenceType(classSymbol.getSuperclass());
    classSymbol.getInterfaces().forEach(this::referenceType);
    return super.visitClass(node, unused);
  }

  @Override
  public Void visitMethod(MethodTree node, Void unused) {
    Symbol.MethodSymbol methodSymbol = ASTHelpers.getSymbol(node);
    referenceSymbol(methodSymbol);
    referenceType(methodSymbol.getReturnType());
    return super.visitMethod(node, unused);
  }

  @Override
  public Void visitVariable(VariableTree node, Void unused) {
    Symbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null) {
      referenceType(symbol.type);
    }
    return super.visitVariable(node, unused);
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree node, Void unused) {
    Symbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null) {
      referenceType(symbol.type);
    }
    return super.visitTypeParameter(node, unused);
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, Void unused) {
    Symbol symbol = ASTHelpers.getSymbol(node.getAnnotationType());
    if (symbol != null) {
      referenceSymbol(symbol);
      referenceType(symbol.type);
    }
    return super.visitAnnotation(node, unused);
  }

  @Override
  public Void visitNewClass(NewClassTree node, Void unused) {
    Symbol.MethodSymbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null) {
      referenceType(symbol.owner.type);
    }
    return super.visitNewClass(node, unused);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
    MethodSymbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null) {
      referenceSymbol(symbol);

      /* The compiler needs to look at every overloaded method in the enclosing
      method to find the correct method to apply at method invocation. This means
      we need to reference as imported all the return types and argument types
      of every overload of the invoked method because those class needs to be
      loaded at compile time. */

      ClassSymbol parentClass = symbol.enclClass();
      var parentClassMembers = parentClass.members();
      if (parentClassMembers != null) {
        // overloaded methods have the same qualified name as the current symbol
        var overloadSymbols =
           StreamSupport.stream( parentClassMembers
                .getSymbols().spliterator(), false)
                // https://github.com/bazel-contrib/unused-jvm-deps/issues/20
                // Apply the filter after the fact so that we're not exposed
                // to the Filter -> Predicate API change.
                .filter(s -> s.getQualifiedName().contentEquals(symbol.getQualifiedName()))
                .collect(Collectors.toList());
        for (Symbol overloadSymbol : overloadSymbols) {
          if (overloadSymbol instanceof MethodSymbol) {
            MethodSymbol overloadMethodSymbol = (MethodSymbol) overloadSymbol;
            referenceType(overloadMethodSymbol.getReturnType());
            if (overloadMethodSymbol.type instanceof MethodType) {
              MethodType overloadedMethodType = (MethodType) overloadMethodSymbol.type;
              if (overloadedMethodType.argtypes != null) {
                for (Type type : overloadedMethodType.argtypes) {
                  referenceType(type);
                }
              }
            }
          }
        }
      }
    }
    return super.visitMethodInvocation(node, unused);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, Void unused) {
    Symbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null) {
      referenceSymbol(symbol);
      referenceType(symbol.type);
    }
    return super.visitMemberSelect(node, unused);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void unused) {
    Symbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null) {
      referenceSymbol(symbol);
      referenceType(symbol.type);
    }
    return super.visitIdentifier(node, unused);
  }

  private JavaFileObject getCurrentSourceFile() {
    return Objects.requireNonNull(currentCompilationUnit).getSourceFile();
  }

  private void referenceType(Type type) {
    if (type == null) {
      return;
    }
    referenceSymbol(type.tsym);
    if (type instanceof ClassType) {
      ClassType classType = (ClassType) type;
      referenceType(classType.getEnclosingType());
      referenceType(classType.supertype_field);
      Set<Type> interfaces = new HashSet<>();
      if (classType.interfaces_field != null) {
        interfaces.addAll(classType.interfaces_field);
      }
      /* for some reason all_interfaces_field is not a superset of interfaces_field */
      if (classType.all_interfaces_field != null) {
        interfaces.addAll(classType.all_interfaces_field);
      }
      for (Type interfaceType : interfaces) {
        referenceType(interfaceType);
      }
    }
  }

  private void referenceSymbol(Symbol symbol) {
    if (isImportedSymbol(symbol)) {
      collector.addImportedSymbol(symbol);
    }
  }

  private boolean isImportedSymbol(Symbol symbol) {
    JavaFileObject originalSourceFile = getSourceFileForSymbol(symbol);
    return symbol != null
        && !isPrimitive(symbol) // we don't care about symbols for primitive types
        && !(symbol instanceof PackageSymbol) // we don't care about package symbols
        && originalSourceFile != getCurrentSourceFile(); // imported means defined in another file
  }

  private boolean isPrimitive(Symbol symbol) {
    /*
     * We want to ignore symbols representing the primitive types themselves, but we don't want to ignore symbols
     * representing a variable of a primitive type. For example, we want to say that
     * the ClassSymbol `int` is a primitive and should be ignored, but we don't want to ignore
     * VarSymbol `example.Class.counter` of type `int`
     */
    return symbol instanceof Symbol.ClassSymbol
        && (symbol.type instanceof Type.JCPrimitiveType
            || symbol.type instanceof Type.JCVoidType
            || symbol.owner == null
            || symbol.owner.kind == Kind.NIL); // ignore 'Method' type which has NIL owner kind;
  }

  private JavaFileObject getSourceFileForSymbol(Symbol symbol) {
    if (symbol == null) {
      return null;
    }
    return (symbol instanceof Symbol.ClassSymbol)
        ? ((ClassSymbol) symbol).sourcefile // class symbols store their source file
        : getSourceFileForSymbol(symbol.owner);
  }
}
