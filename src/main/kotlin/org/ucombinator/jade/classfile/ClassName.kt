package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.`type`.ClassOrInterfaceType

object ClassName {
  fun className(string: String): Name {
    return string.split('/').fold<String, Name?>(null) { qualifier, identifier -> Name(qualifier, identifier) }!!
  }

  fun classNameExpr(string: String): Expression =
    string.split('/').fold(null) { qualifier, identifier ->
      when (qualifier) {
        null -> return NameExpr(SimpleName(identifier))
        else -> return FieldAccessExpr(qualifier, /*TODO*/ NodeList(), SimpleName(identifier))
      }
    }!!

  fun classNameType(string: String): ClassOrInterfaceType? =
    classNameType(className(string))

  fun classNameType(name: Name?): ClassOrInterfaceType? =
    when (name) {
      null -> null
      else -> ClassOrInterfaceType(classNameType(name.getQualifier().orElse(null)), name.getIdentifier())
    }
}
