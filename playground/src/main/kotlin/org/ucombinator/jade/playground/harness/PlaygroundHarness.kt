package org.ucombinator.jade.playground.harness

import java.io.File
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * A simple interface for plug-and-play playground harnesses.
 * Each harness receives a compiled [ClassNode] and a selected [MethodNode]
 * and can stream output to both stdout and a file using [print]/[println].
 */
interface PlaygroundHarness {
    val key: String
    val description: String
    val inputFile: File
    val outputFile: File

    fun run(classNode: ClassNode, methodNode: MethodNode)

    fun print(text: String) {
        kotlin.io.print(text)
        ensureOutputDir()
        outputFile.appendText(text)
    }

    fun println(text: String = "") {
        kotlin.io.println(text)
        ensureOutputDir()
        outputFile.appendText(text + System.lineSeparator())
    }

    fun ensureOutputDir() {
        outputFile.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
    }
}
