package org.ucombinator.jade.playground.harness

import com.github.javaparser.ast.body.MethodDeclaration
import java.io.File
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.ucombinator.jade.decompile.DecompileClass
import org.ucombinator.jade.decompile.DecompileMethodBody
import org.ucombinator.jade.decompile.OptimizeMethodBody

class DecompileHarness() : PlaygroundHarness() {
    override val key: String = "decompile"
    override val description: String = "Decompile a method body and print decompiled and optimized Java code"

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val dummyMethod = DecompileClass.decompileMethod(classNode, methodNode)
        require(dummyMethod is MethodDeclaration) { "Decompiled method is not a MethodDeclaration" }

        val decompiledBody = DecompileMethodBody.decompileBody(classNode, methodNode, dummyMethod)
        val optimizedBody = OptimizeMethodBody.optimize(decompiledBody)

        println(decompiledBody.toString())
    }
}
