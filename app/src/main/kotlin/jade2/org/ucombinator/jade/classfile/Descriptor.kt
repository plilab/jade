package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.`type`.ClassOrInterfaceType
import com.github.javaparser.ast.`type`.Type
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName

object Descriptor {
  fun fieldDescriptor(string: String): Type = Signature.typeSignature(string)

  fun methodDescriptor(string: String): Pair<List<Type>, Type> {
    val s = Signature.methodSignature(string)
    assert(s._1.isEmpty())
    assert(s._4.isEmpty())
    return Pair(s._2, s._3)
  }
}
