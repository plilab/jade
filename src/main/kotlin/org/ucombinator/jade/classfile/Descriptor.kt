package org.ucombinator.jade.classfile

import com.github.javaparser.ast.type.Type

data class MethodDescriptor(val parameterTypes: List<Type>, val returnType: Type)

object Descriptor {
  // TODO: use org.objectweb.asm.Type.getType(descriptor)
  fun fieldDescriptor(string: String): Type = Signature.typeSignature(string)

  // TODO: use org.objectweb.asm.Type.getArgumentTypes(descriptor) and org.objectweb.asm.Type.getReturnType(descriptor)
  fun methodDescriptor(string: String): MethodDescriptor {
    val s = Signature.methodSignature(string)
    require(s.typeParameters.isEmpty()) {
      """Unexpected type parameters "${s.typeParameters}" in descriptor "${string}"."""
    }
    require(s.exceptionTypes.isEmpty()) {
      """Unexpected exception types "${s.exceptionTypes}" in descriptor "${string}"."""
    }
    return MethodDescriptor(s.parameterTypes, s.returnType)
  }
}
