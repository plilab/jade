package org.ucombinator.jade.playground.harness

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import java.io.File
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.ucombinator.jade.decompile.DecompileClass
import org.ucombinator.jade.decompile.DecompileMethodBody
import org.ucombinator.jade.optimize.ControlFlowOptimizer
import org.ucombinator.jade.optimize.EmptyStatementOptimizer
import org.ucombinator.jade.playground.util.toDot

class VisualizeDecompileHarness() : PlaygroundHarness {
    override var inputFile: File = File("")
    override val key: String = "visualize-decompile"
    override var extension: String = "dot"
    override val description: String = "Decompile a method body and print decompiled and optimized Java code"

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val dummyMethod = DecompileClass.decompileMethod(classNode, methodNode)
        require(dummyMethod is MethodDeclaration) { "Decompiled method is not a MethodDeclaration" }

        val decompiledBody: BlockStmt = DecompileMethodBody.decompileBody(classNode, methodNode, dummyMethod)

        print(decompiledBody.toDot())
    }

    // Uses default implementation in PlaygroundHarness

    companion object {
        const val KEY: String = "visualize-decompile"
        const val DESCRIPTION: String = "Decompile a method body and save hierachy to dot file"
    }
}
