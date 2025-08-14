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
    var inputFile: File

    fun run(classNode: ClassNode, methodNode: MethodNode)

    fun withInput(file: File): PlaygroundHarness {
        this.inputFile = file
        return this
    }

    fun print(text: String) {
        kotlin.io.print(text)
        outputFile().appendText(text)
    }

    fun println(text: String = "") {
        kotlin.io.println(text)
        outputFile().appendText(text + System.lineSeparator())
    }

    fun outputFile(): File {
        val dir = File("output/${key}")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${inputFile.nameWithoutExtension}.txt")
    }
}
