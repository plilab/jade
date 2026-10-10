package org.ucombinator.jade.test

import javax.tools.ToolProvider
import java.io.File
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll

// This is the command to do testing
// To use this command, 
// Create a directory at  src/test/classFiles
// Then compile the testcases find src/test/resources -name "*.java" -exec javac -d src/test/classFiles {} \;
// Then type jade test or ./gradlew run --args="test" in the terminal
// The testing process is 
// Read Bytecode .class file -> decompile using Jade -> recompile using Javac -> Compare the bytecode
// Test is considered pass if the two bytecode are the same
class mainTest {

    // Prepare the logging file for test
    // Create an empty output.txt file, overwrite if exists
    companion object {
        @JvmStatic
        @BeforeAll
        fun prepareLog() {
            val outputFile = File("output.txt")
            outputFile.writeText("")
        }
    }

    @Test
    fun decompileTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            assertTrue(DecompileTest.testone(files))
            }
        }
    
}