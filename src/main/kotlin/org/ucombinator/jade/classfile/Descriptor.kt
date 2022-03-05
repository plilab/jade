package org.ucombinator.jade.classfile

import com.github.javaparser.ast.type.Type

object Descriptor {
  fun fieldDescriptor(string: String): Type = Signature.typeSignature(string)

  fun methodDescriptor(string: String): Pair<List<Type>, Type> {
    val s = Signature.methodSignature(string)
    assert(s._1.isEmpty())
    assert(s._4.isEmpty())
    return Pair(s._2, s._3)
  }
}
