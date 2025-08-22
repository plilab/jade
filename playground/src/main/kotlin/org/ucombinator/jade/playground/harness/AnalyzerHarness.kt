package org.ucombinator.jade.playground.harness

import java.io.File
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame

class AnalyzerHarness() : PlaygroundHarness() {
    override val key: String = "webasm-analyzer"
    override val description: String = "Run ASM BasicInterpreter + Analyzer over a method and summarize frames"

    override fun run(classNode: ClassNode, methodNode: MethodNode) {
        val interpreter = BasicInterpreter()
        val analyzer = Analyzer(interpreter)
        try {
            val frames: Array<Frame<BasicValue>?> = analyzer.analyze(classNode.name, methodNode)
            println("Method: ${methodNode.name}")
            println("Number of instructions: ${methodNode.instructions.size()}")
            println("Number of frames: ${frames.size}")
            for (i in frames.indices) {
                val frame = frames[i]
                if (frame != null) {
                    println("Frame $i: locals=${frame.locals} stack=${frame.stackSize}")
                } else {
                    println("Frame $i: unreachable")
                }
            }
        } catch (e: AnalyzerException) {
            println("Analysis failed: ${e.message}")
            println(e.stackTraceToString())
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println(e.stackTraceToString())
        }
    }
}
