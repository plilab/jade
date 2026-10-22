package org.ucombinator.jade.decompile

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.SuperExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.TypeParameter

/**
 * Replaces unambiguous fully qualified type references with simple names and adds the imports
 * required by those names.
 *
 * Import decisions are made for the whole compilation unit before the AST is changed. This keeps
 * references qualified when their simple name denotes multiple types or conflicts with a declared
 * type or type parameter.
 */
@Suppress("ktlint:standard:no-unit-return")
internal object OrganizeImports {
  private const val JAVA_LANG_PACKAGE = "java.lang"

  /** A qualified AST reference together with the mutation that converts it to a simple name. */
  private data class TypeReference(
    val qualifiedName: String,
    val shorten: () -> Unit,
  ) {
    val simpleName: String
      get() = qualifiedName.substringAfterLast('.')
  }

  /** Names declared in the compilation unit that can shadow an imported simple type name. */
  private data class DeclaredNames(
    val nestedTypes: Set<String>,
    val topLevelTypes: Map<String, String>,
    val typeParameters: Set<String>,
  ) {
    fun conflictsWith(simpleName: String, qualifiedName: String): Boolean =
      simpleName in nestedTypes ||
        simpleName in typeParameters ||
        topLevelTypes[simpleName]?.let { it != qualifiedName } == true
  }

  /**
   * Organizes the imports of [compilationUnit] in place.
   *
   * Qualified references are shortened only when every occurrence of their simple name resolves to
   * the same qualified name and that name does not conflict with a declaration in the compilation
   * unit. Existing imports are retained, deduplicated, and sorted with newly generated imports.
   * Types in the current package and `java.lang` are shortened without adding imports.
   */
  fun organizeImports(compilationUnit: CompilationUnit): Unit {
    val references = collectTypeReferences(compilationUnit)
    val declaredNames = collectDeclaredNames(compilationUnit)
    val importedNames = compilationUnit.imports
      .filter(::isExplicitTypeImport)
      .map { it.nameAsString }

    // Include existing explicit imports in collision analysis even when the imported type is not
    // referenced by the AST. Introducing another import with the same simple name would be illegal.
    val qualifiedNames = (references.map { it.qualifiedName } + importedNames)
    val qualifiedNamesBySimpleName = qualifiedNames
      .groupBy { it.substringAfterLast('.') }
      .mapValues { (_, names) -> names.toSet() }
    val unambiguousNames = qualifiedNamesBySimpleName
      .filter { (simpleName, qualifiedNames) ->
        qualifiedNames.size == 1 && !declaredNames.conflictsWith(simpleName, qualifiedNames.single())
      }.mapValues { (_, qualifiedNames) -> qualifiedNames.single() }

    references
      .filter { unambiguousNames[it.simpleName] == it.qualifiedName }
      .forEach { it.shorten() }

    val currentPackage = compilationUnit.packageDeclaration
      .map { it.nameAsString }
      .orElse("")
    val newImports = unambiguousNames.values
      .filter { requiresImport(it, currentPackage) }
      .map { ImportDeclaration(it, false, false) }
    replaceImports(compilationUnit, compilationUnit.imports + newImports)
  }

  /**
   * Returns whether [type] is only a scope segment of another class or interface type.
   *
   * JavaParser represents `java.util.List` as a chain of [ClassOrInterfaceType] nodes. Only the
   * outermost `List` node is a reference to organize; visiting `java` and `util` independently would
   * incorrectly treat package components as types.
   */
  private fun isScopeComponent(type: ClassOrInterfaceType): Boolean {
    val parent = type.parentNode.orElse(null)
    return parent is ClassOrInterfaceType && parent.scope.orElse(null) === type
  }

  /**
   * Returns whether removing [type]'s complete scope would discard type syntax.
   *
   * For example, neither `Outer<T>.Inner` nor `@A Outer.Inner` may be reduced directly to `Inner`.
   * They could be shortened partially by importing `Outer`, but this organizer deliberately leaves
   * the complete reference qualified until partial scope shortening is supported.
   */
  private fun hasParameterizedOrAnnotatedScope(type: ClassOrInterfaceType): Boolean =
    generateSequence(type.scope.orElse(null)) { it.scope.orElse(null) }
      .any { it.typeArguments.isPresent || it.annotations.isNotEmpty() }

  /** Returns whether [type] is a complete qualified reference that can be safely shortened. */
  private fun isClassOrInterfaceReference(type: ClassOrInterfaceType): Boolean =
    type.scope.isPresent &&
      !isScopeComponent(type) &&
      !hasParameterizedOrAnnotatedScope(type)

  /**
   * Collects qualified type references from every supported JavaParser representation.
   *
   * Most type uses are [ClassOrInterfaceType] nodes. Annotation names and qualified `Interface.super`
   * expressions use different node types, so each supplies its own deferred shortening operation.
   * Deferring all mutations allows [organizeImports] to detect collisions before changing the AST.
   */
  private fun collectTypeReferences(compilationUnit: CompilationUnit): List<TypeReference> {
    val classOrInterfaceReferences = compilationUnit
      .findAll(ClassOrInterfaceType::class.java)
      .filter(::isClassOrInterfaceReference)
      .map { TypeReference(it.nameWithScope) { it.removeScope() } }

    val annotationReferences = compilationUnit
      .findAll(AnnotationExpr::class.java)
      .filter { it.name.qualifier.isPresent }
      .map {
        TypeReference(it.nameAsString) {
          it.setName(Name(it.name.identifier))
        }
      }

    val interfaceSuperReferences = compilationUnit
      .findAll(SuperExpr::class.java)
      .filter {
        it.typeName
          .orElse(null)
          ?.qualifier
          ?.isPresent == true
      }.mapNotNull {
        it.typeName
          .map { typeName ->
            TypeReference(typeName.asString()) {
              it.setTypeName(Name(typeName.identifier))
            }
          }.orElse(null)
      }

    return buildList {
      addAll(classOrInterfaceReferences)
      addAll(annotationReferences)
      addAll(interfaceSuperReferences)
    }
  }

  /** Collects declarations whose names could change the meaning of a shortened type reference. */
  private fun collectDeclaredNames(compilationUnit: CompilationUnit): DeclaredNames {
    val typeDeclarations = compilationUnit.findAll(TypeDeclaration::class.java)
    val packageName = compilationUnit.packageDeclaration.map { it.nameAsString }.orElse("")
    val topLevelTypes = typeDeclarations
      .filter { it.isTopLevelType }
      .associate { declaration ->
        val qualifiedName = declaration.fullyQualifiedName.orElseGet {
          listOf(packageName, declaration.nameAsString).filter(String::isNotEmpty).joinToString(".")
        }
        declaration.nameAsString to qualifiedName
      }
    val nestedTypes = typeDeclarations
      .filter { it.isNestedType }
      .mapTo(mutableSetOf()) { it.nameAsString }
    val typeParameters = compilationUnit
      .findAll(TypeParameter::class.java)
      .mapTo(mutableSetOf()) { it.nameAsString }

    return DeclaredNames(nestedTypes, topLevelTypes, typeParameters)
  }

  /** Returns whether an import introduces exactly one non-static type name. */
  private fun isExplicitTypeImport(importDeclaration: ImportDeclaration): Boolean =
    !importDeclaration.isStatic && !importDeclaration.isAsterisk && !importDeclaration.isModule

  /** Returns whether referring to [qualifiedName] by its simple name requires an import. */
  private fun requiresImport(qualifiedName: String, currentPackage: String): Boolean {
    val packageName = qualifiedName.substringBeforeLast('.', "")
    return packageName != currentPackage && packageName != JAVA_LANG_PACKAGE
  }

  /** Replaces the import list with detached, deduplicated imports in deterministic order. */
  private fun replaceImports(compilationUnit: CompilationUnit, imports: List<ImportDeclaration>): Unit {
    val sortedImports = imports
      .distinctBy { importDeclaration ->
        listOf(
          importDeclaration.nameAsString,
          importDeclaration.isStatic.toString(),
          importDeclaration.isAsterisk.toString(),
          importDeclaration.isModule.toString(),
        )
      }.sortedWith(
        compareBy<ImportDeclaration>(
          { it.nameAsString },
          { it.isStatic },
          { it.isAsterisk },
          { it.isModule },
        ),
      ).map { it.clone() }

    compilationUnit.imports.clear()
    compilationUnit.imports.addAll(sortedImports)
  }
}
