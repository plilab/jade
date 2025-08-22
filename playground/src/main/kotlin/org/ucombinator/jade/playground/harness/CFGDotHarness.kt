package org.ucombinator.jade.playground.harness

import java.io.File
import org.ucombinator.jade.analysis.StaticSingleAssignment
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.ucombinator.jade.playground.util.toDot

class CFGDotHarness() : PlaygroundHarness() {
    override val key: String = "cfg-dot"
    override val description: String = "Run CFG analysis over a method and print the result as a DOT file"

    init {
        extension = "dot"
    }

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val cfg = ControlFlowGraph.make(classNode.name, methodNode)
        val dot = cfg.toDot()
        println(dot)
    }
}
