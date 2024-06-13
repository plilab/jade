package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.ClassOrInterfaceType

/** TODO:doc. */
object ClassName {
  /** TODO:doc.
   *
   * @param identifier TODO:doc
   * @return TODO:doc
   */
  fun identifier(identifier: String): String =
    identifier.apply {
      require(this.isNotEmpty()) { "Empty identifier" }
      require(!this.any { it in ".;[/<>:" }) { """Invalid identifier "${this}"""" }
    }

  /** TODO:doc.
   *
   * @param string TODO:doc
   * @return TODO:doc
   */
  fun identifiers(string: String): List<String> = string.split('/').map(::identifier)

  /** TODO:doc.
   *
   * @param string TODO:doc
   * @return TODO:doc
   */
  fun className(string: String): Name = identifiers(string).fold(null, ::Name)!!

  /** TODO:doc.
   *
   * @param string TODO:doc
   * @return TODO:doc
   */
  fun classNameExpr(string: String): Expression =
    identifiers(string).map(::SimpleName).fold(null as Expression?) { qualifier, simpleName ->
      if (qualifier == null) NameExpr(simpleName) else FieldAccessExpr(qualifier, /*TODO*/ NodeList(), simpleName)
    }!!

  /** TODO:doc.
   *
   * @param string TODO:doc
   * @return TODO:doc
   */
  fun classNameType(string: String): ClassOrInterfaceType = classNameType(className(string))

  /** TODO:doc.
   *
   * @param name TODO:doc
   * @return TODO:doc
   */
  fun classNameType(name: Name): ClassOrInterfaceType = classNameTypeOrNull(name)!!

  /** TODO:doc.
   *
   * @param name TODO:doc
   * @return TODO:doc
   */
  private fun classNameTypeOrNull(name: Name?): ClassOrInterfaceType? =
    name?.let { ClassOrInterfaceType(classNameTypeOrNull(it.qualifier.orElse(null)), it.identifier) }
}
