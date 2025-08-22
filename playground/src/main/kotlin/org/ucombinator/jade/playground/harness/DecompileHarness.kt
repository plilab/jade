package org.ucombinator.jade.playground.harness

import com.github.javaparser.ast.body.MethodDeclaration
import java.io.File
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.ucombinator.jade.decompile.DecompileClass
import org.ucombinator.jade.decompile.DecompileMethodBody
import org.ucombinator.jade.decompile.OptimizeMethodBody

class DecompileHarness() : PlaygroundHarness {
    override var inputFile: File = File("")
    override val key: String = "decompile"
    override val extension = "txt"
    override val description: String = "Decompile a method body and print decompiled and optimized Java code"

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val dummyMethod = DecompileClass.decompileMethod(classNode, methodNode)
        require(dummyMethod is MethodDeclaration) { "Decompiled method is not a MethodDeclaration" }

        val decompiledBody = DecompileMethodBody.decompileBody(classNode, methodNode, dummyMethod)
        val optimizedBody = OptimizeMethodBody.optimize(decompiledBody)

        println("Decompiled Body:")
        println(decompiledBody.toString())
        println()
        println("Optimized Body:")
        println(optimizedBody.toString())
    }

    // Uses default implementation in PlaygroundHarness

    companion object {
        const val KEY: String = "decompile"
        const val DESCRIPTION: String = "Decompile a method body and print decompiled and optimized Java code"
    }
}
