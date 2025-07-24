package org.ucombinator.jade.playground

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.ucombinator.jade.decompile.DecompileMethodBody
import org.ucombinator.jade.decompile.OptimizeMethodBody
import java.io.File
import org.ucombinator.jade.decompile.DecompileClass
import com.github.javaparser.ast.body.MethodDeclaration

fun testMethodBodyDecompilation(classFile: File) {
    val classReader = ClassReader(classFile.inputStream())
    val classNode = ClassNode()
    classReader.accept(classNode, 0)

    val mainMethodNode = classNode.methods.find { it.name == "main" }
    require(mainMethodNode != null) { "Could not find main method in ${classFile.name}" }

    // Use decompileMethod to get the MethodDeclaration directly
    val dummyMethod = DecompileClass.decompileMethod(classNode, mainMethodNode)
    require(dummyMethod is MethodDeclaration) { "Decompiled method is not a MethodDeclaration" }

    println("Decompiling method body for 'main' in ${classNode.name}...")
    val decompiledBody = DecompileMethodBody.decompileBody(classNode, mainMethodNode, dummyMethod)

    println("\n" + "=".repeat(50))
    println("RAW DECOMPILED METHOD BODY:")
    println("=".repeat(50))
    println(decompiledBody.toString())

    // Apply optimizations
    val optimizedBody = OptimizeMethodBody.optimize(decompiledBody)

    println("\n" + "=".repeat(50))
    println("OPTIMIZED METHOD BODY:")
    println("=".repeat(50))
    println(optimizedBody.toString())
} 