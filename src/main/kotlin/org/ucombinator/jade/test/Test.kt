package org.ucombinator.jade.test

import javax.tools.ToolProvider
import java.io.File
import java.io.StringWriter
import org.junit.jupiter.api.BeforeAll

// This is the command to do testing
// To use this command, 
// Create a directory at  src/test/classFiles
// Then compile the testcases find src/test/resources -name "*.java" -exec javac -d src/test/classFiles {} \;
// Then type jade test or ./gradlew run --args="test" in the terminal
// The testing process is 
// Read Bytecode .class file -> decompile using Jade -> recompile using Javac -> Compare the bytecode
// Test is considered pass if the two bytecode are the same
object DecompileTest {

    fun main(files: List<File>) {
        testone(files)
    }

    // The auxiliary funtion that process one test case
    fun testone (files: List<File>) : Boolean {
        val outputFile = File("output.txt")

        val originalByteCode: HashMap<String, ByteArray> = HashMap<String, ByteArray>()
        for (file in files) {
            originalByteCode[file.name.removeSuffix(".class")] = file.readBytes()
        }

        val result = org.ucombinator.jade.decompile.Decompile.decompile(files)

        val cu = mutableListOf<StringJavaFileObject>()

        for ((name, code) in result) {
            outputFile.appendText("File name: ${name}\n")
            outputFile.appendText("File content: ${code}\n")
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

        outputFile.appendText("Compilation successful: $success\n")
        var allSame = false

        if (success) {
            // Verify the recompiled files against the initial files one by one
            val recompiledBytecodeFiles = HashMap<String, ByteArray>()

            for ((className, file) in fileManager.bytecodeFiles) {
                recompiledBytecodeFiles[className] = file.output.toByteArray()
            }

            outputFile.appendText("Original ${originalByteCode.toString()}\n")
            outputFile.appendText("Recompiled ${recompiledBytecodeFiles.toString()}\n")

            allSame = true


            for ((name, bytecode) in originalByteCode) {
                if (recompiledBytecodeFiles[name] == null) {
                    allSame = false
                    outputFile.appendText("Target file ${name} is not produced during recompilation\n")
                } else {
                    if (!bytecode.contentEquals(recompiledBytecodeFiles[name])) {
                        outputFile.appendText("The recompiled bytecode file ${name} is different from initial\n")
                        allSame = false
                    }
                }
            }

            if (allSame) {
                outputFile.appendText("Testcase success! The recompiled bytecode is the same as initial\n")
            }

        } else {
            outputFile.appendText(err.toString() + "\n")
            
        }
            fileManager.close()
            return allSame
        }

}