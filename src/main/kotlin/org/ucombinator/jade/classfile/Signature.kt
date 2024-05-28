package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.*
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Tuples.Fourple
import org.objectweb.asm.util.TraceSignatureVisitor
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.7.9.1
// https://gitlab.ow2.org/asm/asm/-/blob/master/asm/src/main/java/org/objectweb/asm/signature/SignatureReader.java
// https://github.com/openjdk/jdk/blob/jdk-23%2B23/src/java.base/share/classes/sun/reflect/generics/parser/SignatureParser.java

// TODO: use these instead of Triple and Fourple
data class ClassSignature(val typeParameters: List<TypeParameter>, val superclass: ClassOrInterfaceType, val interfaces: List<ClassOrInterfaceType>)
data class MethodSignature(val typeParameters: List<TypeParameter>, val parameterTypes: List<Type>, val returnType: Type, val exceptionTypes: List<ReferenceType>)

object Signature {
  fun typeSignature(string: String): Type {
    // Check that SignatureReader parses to the end of the string
    val signatureWriter = SignatureWriter()
    SignatureReader(string).acceptType(signatureWriter)
    if (string != signatureWriter.toString()) TODO()

    var result = null as Type? // TODO
    SignatureReader(string).acceptType(DelegatingSignatureVisitor(TypeSignatureVisitor({ result = it; null })))
    return result!!
  }

  fun classSignature(string: String): Triple<List<TypeParameter>, ClassOrInterfaceType, List<ClassOrInterfaceType>> {
    // Check that SignatureReader parses to the end of the string
    val signatureWriter = SignatureWriter()
    SignatureReader(string).accept(signatureWriter)
    if (string != signatureWriter.toString()) TODO()

    val visitor = ClassSignatureVisitor()
    SignatureReader(string).accept(DelegatingSignatureVisitor(visitor))
    return Triple(visitor.typeParameters, visitor.superclass!!, visitor.interfaces)
  }

  // TODO: rename arg to signature
  fun methodSignature(string: String): Fourple<List<TypeParameter>, List<Type>, Type, List<ReferenceType>> {
    // Check that SignatureReader parses to the end of the string
    val signatureWriter = SignatureWriter()
    SignatureReader(string).accept(signatureWriter)
    if (string != signatureWriter.toString()) TODO()

    val visitor = MethodSignatureVisitor()
    SignatureReader(string).accept(DelegatingSignatureVisitor(visitor))
    return Fourple(visitor.typeParameters, visitor.parameterTypes, visitor.returnType!!, visitor.exceptionTypes)
  }
  // TODO: ktlint: allow /////////////////
}
