package org.ucombinator.jade.classfile

import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.TypeExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType

/** Converts slash-separated JVM internal class names into JavaParser name and type nodes. */
object ClassName {

  /**
   * Validates and returns one component of a JVM internal class name.
   *
   * @param identifier an unqualified class or package-name component.
   * @return `identifier`, unchanged.
   * @throws IllegalArgumentException if `identifier` is empty or contains JVM name delimiters.
   */
  fun identifier(identifier: String): String =
    identifier.apply {
      require(this.isNotEmpty()) { "Empty identifier" }
      require(!this.any { it in ".;[/<>:" }) { """Invalid identifier "${this}"""" }
    }

  /**
   * Splits a JVM internal class name into validated package and class-name components.
   *
   * @param name a slash-separated JVM internal class name, such as `java/lang/String`.
   * @return the components of `name`, such as `listOf("java", "lang", "String")`.
   * @throws IllegalArgumentException if any component is invalid.
   */
  fun identifiers(name: String): List<String> = name.split('/').map(::identifier)

  /**
   * Creates a qualified JavaParser name from a JVM internal class name.
   *
   * @param name a slash-separated JVM internal class name, such as `java/lang/String`.
   * @return the corresponding qualified name, such as `java.lang.String`.
   */
  fun className(name: String): Name = identifiers(name).fold(null, ::Name)!!

  /**
   * Creates an expression that denotes a class from its JVM internal name.
   *
   * A [TypeExpr] preserves that the expression denotes a type, allowing later AST passes to
   * distinguish a static owner such as `java.lang.Integer` from an ordinary expression chain.
   *
   * @param name a slash-separated JVM internal class name, such as `java/lang/Integer`.
   * @return a type expression for the corresponding class.
   */
  fun classNameExpr(name: String): TypeExpr = classNameExpr(classNameType(name))

  /**
   * Creates an expression that denotes the supplied class or interface type.
   *
   * @param name the class or interface type represented by the expression.
   * @return a type expression containing `name`.
   */
  fun classNameExpr(name: ClassOrInterfaceType): TypeExpr = TypeExpr(name)

  /**
   * Creates a JavaParser class or interface type from a JVM internal class name.
   *
   * @param name a slash-separated JVM internal class name, such as `java/lang/String`.
   * @return the corresponding class or interface type, such as `java.lang.String`.
   */
  fun classNameType(name: String): ClassOrInterfaceType = classNameType(className(name))

  /**
   * Converts a qualified JavaParser name into a class or interface type.
   *
   * @param name the qualified name to convert.
   * @return a class or interface type with the same components as `name`.
   */
  fun classNameType(name: Name): ClassOrInterfaceType = classNameTypeOrNull(name)!!

  /**
   * Recursively converts a possibly absent qualified name into a class or interface type.
   *
   * @param name the qualified name to convert, or `null` at the end of the recursion.
   * @return the corresponding class or interface type, or `null` when `name` is `null`.
   */
  private fun classNameTypeOrNull(name: Name?): ClassOrInterfaceType? =
    name?.let { ClassOrInterfaceType(classNameTypeOrNull(it.qualifier.orElse(null)), it.identifier) }
}
