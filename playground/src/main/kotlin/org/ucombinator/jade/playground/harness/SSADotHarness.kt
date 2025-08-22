package org.ucombinator.jade.playground.harness

import java.io.File
import org.ucombinator.jade.analysis.StaticSingleAssignment
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class SSADotHarness() : PlaygroundHarness() {
    override val key: String = "ssa-dot"
    override val description: String = "Run SSA analysis and describe result"

    init {
        extension = "dot"
    }

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        
        val cfg = ControlFlowGraph.make(classNode.name, methodNode)
        val ssa = StaticSingleAssignment.make(classNode.name, methodNode, cfg)
        
        println("digraph SSA {")
        
        ssa.phiInputs.forEach { (varLeft, setOfPhiInputs) ->
            setOfPhiInputs.forEach { (_, varRight) ->
                println("  \"${varRight?.name ?: varRight.toString()}\" -> \"${varLeft?.name ?: varLeft.toString()}\" [label=\"depends on\"];")
            }
        }
        
        println("}")
    }
}
