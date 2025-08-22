package org.ucombinator.jade.playground.harness

import java.io.File
import org.ucombinator.jade.analysis.StaticSingleAssignment
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class SSATestHarness() : PlaygroundHarness {
    override var inputFile: File = File("")
    override val key: String = "ssa-test"
    override var extension: String = "txt"
    override val description: String = "Run SSA analysis over a method and print the result"

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val cfg = ControlFlowGraph.make(classNode.name, methodNode)
        val ssa = StaticSingleAssignment.make(classNode.name, methodNode, cfg)
        println(ssa.toString())
    }

    // Uses default implementation in PlaygroundHarness

    companion object {
        const val KEY: String = "ssa-test"
        const val DESCRIPTION: String = "Run SSA analysis over a method and summarize frames"
    }
}
