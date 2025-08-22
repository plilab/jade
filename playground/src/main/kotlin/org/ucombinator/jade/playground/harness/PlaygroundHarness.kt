package org.ucombinator.jade.playground.harness

import java.io.File
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * A simple abstract base class for plug-and-play playground harnesses.
 * Each harness receives a compiled [ClassNode] and a selected [MethodNode]
 * and can stream output to both stdout and a file using [print]/[println].
 */
abstract class PlaygroundHarness {
    abstract val key: String
    abstract val description: String
    lateinit var inputFile: File
    var extension: String = "txt"

    abstract fun run(classNode: ClassNode, methodNode: MethodNode)

    fun withInput(file: File): PlaygroundHarness {
        this.inputFile = file
        resetOutputFile()
        return this
    }

    fun print(text: String) {
        outputFile().appendText(text)
    }

    fun println(text: String = "") {
        outputFile().appendText(text + System.lineSeparator())
    }

    fun resetOutputFile() {
        if (outputFile().exists()) outputFile().delete()
    }

    fun outputFile(): File {
        val dir = File("output/${key}")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${inputFile.nameWithoutExtension}.${extension}")
    }
}
