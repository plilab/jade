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

class DecompileHarness() : PlaygroundHarness {
    override var inputFile: File = File("")
    override val key: String = "decompile"
    override var extension: String = "txt"
    override val description: String = "Decompile a method body and print decompiled and optimized Java code"

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        println("=== Decompile Harness Demo ===")
        println("Class: ${classNode.name}")
        println("Method: ${methodNode.name}${methodNode.desc}")
        println()

        val dummyMethod = DecompileClass.decompileMethod(classNode, methodNode)
        require(dummyMethod is MethodDeclaration) { "Decompiled method is not a MethodDeclaration" }

        val decompiledBody: BlockStmt = DecompileMethodBody.decompileBody(classNode, methodNode, dummyMethod)
        
        println("=== Original Decompiled Code ===")
        println(decompiledBody.toString())
        println()

        // Showcase the new optimizer design with chaining
        println("=== Creating Optimization Pipeline ===")
        val optimizationPipeline = EmptyStatementOptimizer()
            .then(ControlFlowOptimizer())
        
        println("Pipeline: ${optimizationPipeline.getChainName()}")
        println()

        // Run optimization and show context passing
        println("=== Running Optimization ===")
        val (optimizedBody, finalContext) = optimizationPipeline.optimize(decompiledBody)
        
        println("=== Optimized Code ===")
        println(optimizedBody.toString())
        println()
        
        println("=== Optimization Context Results ===")
        println("Label references found: ${finalContext.labelReferences.size}")
        if (finalContext.labelReferences.isNotEmpty()) {
            println("Referenced labels: ${finalContext.labelReferences.joinToString(", ")}")
        }
        println("Variable mappings: ${finalContext.variableMap.size}")
        println("Metadata entries: ${finalContext.metadata.size}")
        
        if (finalContext.metadata.isNotEmpty()) {
            println("Metadata keys: ${finalContext.metadata.keys.joinToString(", ")}")
        }
        println()
    }

    // Uses default implementation in PlaygroundHarness

    companion object {
        const val KEY: String = "decompile"
        const val DESCRIPTION: String = "Decompile a method body and print decompiled and optimized Java code"
    }
}
