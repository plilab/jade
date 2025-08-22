package org.ucombinator.jade.playground.harness

import java.io.File
import org.ucombinator.jade.analysis.StaticSingleAssignment
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class SSATestHarness() : PlaygroundHarness() {
    override val key: String = "ssa-test"
    override val description: String = "Run SSA analysis over a method and print the result"

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val cfg = ControlFlowGraph.make(classNode.name, methodNode)
        val ssa = StaticSingleAssignment.make(classNode.name, methodNode, cfg)
        println(ssa.toString())
    }
}
