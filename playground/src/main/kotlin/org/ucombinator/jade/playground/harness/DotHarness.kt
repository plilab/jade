package org.ucombinator.jade.playground.harness

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import java.io.File
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.ucombinator.jade.decompile.DecompileClass
import org.ucombinator.jade.decompile.DecompileMethodBody
import org.ucombinator.jade.playground.util.toDot

class DotHarness() : PlaygroundHarness() {
    override val key: String = "dot"
    override val description: String = "Decompile a method body and visualize hierarchy as DOT graph"

    init {
        extension = "dot"
    }

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val dummyMethod = DecompileClass.decompileMethod(classNode, methodNode)
        require(dummyMethod is MethodDeclaration) { "Decompiled method is not a MethodDeclaration" }

        val decompiledBody: BlockStmt = DecompileMethodBody.decompileBody(classNode, methodNode, dummyMethod)

        print(decompiledBody.toDot())
    }
}
