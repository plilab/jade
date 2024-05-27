package org.ucombinator.jade.classfile

import com.github.javaparser.ast.type.Type

object Descriptor {
  // TODO: use org.objectweb.asm.Type.getType(descriptor)
  fun fieldDescriptor(string: String): Type = Signature.typeSignature(string)

  // TODO: use org.objectweb.asm.Type.getArgumentTypes(descriptor) and org.objectweb.asm.Type.getReturnType(descriptor)
  fun methodDescriptor(string: String): Pair<List<Type>, Type> {
    val s = Signature.methodSignature(string)
    assert(s._1.isEmpty())
    assert(s._4.isEmpty())
    return Pair(s._2, s._3)
  }
}
