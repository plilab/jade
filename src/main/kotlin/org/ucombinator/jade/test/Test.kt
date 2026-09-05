package org.ucombinator.jade.test

import javax.tools.ToolProvider
import java.io.File
import java.io.StringWriter


object Test {
    fun main () {
        val dummyClass = File("./src/main/kotlin/org/ucombinator/jade/test/simpleTest1.class")
        val dummyDir = File("dummy")
        val initialBytes = dummyClass.readBytes()

        val files: List<File> = listOf(dummyClass)
        val result = org.ucombinator.jade.decompile.Decompile.main(files, dummyDir, false)

        val cu = mutableListOf<StringJavaFileObject>()

        for ((name, code) in result) {
            println("File name: ${name}")
            println("File content: ${code}")
            cu.add(StringJavaFileObject(name.removeSuffix(".java"), code))
        }

        val compiler = ToolProvider.getSystemJavaCompiler()

        val standardFileManager = compiler.getStandardFileManager(
        null,
        null,
        null
    )

        val fileManager = InMemoryFileManager(
            standardFileManager
        )
        val err = StringWriter()

        val task = compiler.getTask(
            err,
            fileManager,
            null,
            null,
            null,
            cu
        )

        val success = task.call()

        println("Compilation successful: $success")

        if (success) {
            val bytecode = fileManager.bytecodeFile.output.toByteArray()
            println("Compiled class size: ${bytecode.size} bytes")
            println(bytecode.contentToString())

            if (initialBytes.contentEquals(bytecode)) {
                println("Test success! The recompiled bytecode is the same as initial")
            } else {
                println("The recompiled bytecode is different from initial")
            }
        } else {
            println(err.toString())
        }
            fileManager.close()
        }
}