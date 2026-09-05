package org.ucombinator.jade.test

import javax.tools.ToolProvider
import java.io.File
import java.io.StringWriter

// This is the command to do testing
// To use this command, just type jade test or ./gradlew run --args="test" in the terminal
// The testing process is 
// Read Bytecode .class file -> decompile using Jade -> recompile using Javac -> Compare the bytecode
// Test is considered pass if the two bytecode are the same
object Test {
    fun main () {
        //TODO: Integrate with the testcases written by Leslie
        val dummyClass = File("./src/main/kotlin/org/ucombinator/jade/test/simpleTest1.class")
        // The output directory is not used since the files are not saved to disk during testing
        // So a place holder is used here
        val dummyDir = File("dummy")
        val initialBytes = dummyClass.readBytes()

        val files: List<File> = listOf(dummyClass)
        val result = org.ucombinator.jade.decompile.Decompile.main(files, dummyDir, false)

        val cu = mutableListOf<StringJavaFileObject>()

        for ((name, code) in result) {
            println("File name: ${name}")
            println("File content: ${code}")
            // Here the suffix is remove to prevent compilation failure due to double suffix like test.java.java
            cu.add(StringJavaFileObject(name.removeSuffix(".java"), code))
        }

        // TODO: We may want to fix the version of compiler
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