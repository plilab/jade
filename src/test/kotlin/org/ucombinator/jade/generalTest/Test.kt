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

    // The auxiliary funtion that process one test case
    fun testone (files: List<File>, name: String) {
        // val initialBytes = file.readBytes()
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

        outputFile.appendText("Running Testcase ${name}\n")
        outputFile.appendText("Compilation successful: $success\n")

        if (success) {
            // Verify the recompiled files against the initial files one by one
            val recompiledBytecodeFiles = HashMap<String, ByteArray>()

            for ((className, file) in fileManager.bytecodeFiles) {
                recompiledBytecodeFiles[className] = file.output.toByteArray()
            }

            outputFile.appendText("Original ${originalByteCode.toString()}\n")
            outputFile.appendText("Recompiled ${recompiledBytecodeFiles.toString()}\n")

            var allSame = true

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
                outputFile.appendText("Testcase ${name} success! The recompiled bytecode is the same as initial\n")
            }
            assertTrue(allSame)

        } else {
            outputFile.appendText(err.toString() + "\n")
            assertTrue(false)
        }
            fileManager.close()
        }

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



    // Below are the test function for kotlin unit test
    @Test
    fun simpleTest1 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "Test1"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }

    @Test
    fun simpleTest2 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "Test2"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    @Test
    fun simpleTest3 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")


        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "Test3"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    @Test
    fun simpleTest4 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "Test4"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    @Test
    fun simpleTest5 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "Test5"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    
    @Test
    fun StaticInnerClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "StaticInnerClassTest"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }

    @Test
    fun StaticBlockTest0 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "StaticBlockTest0"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    @Test
    fun StaticBlockTest1 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "StaticBlockTest1"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    @Test
    fun StaticBlockTest2 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "StaticBlockTest2"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    @Test
    fun SampleInterfaceTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "SampleInterface"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    
    @Test
    fun SampleChildClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "SampleChildClass"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    
    @Test
    fun SampleAbstractClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "SampleAbstractClass"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    
    @Test
    fun PublicInnerClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "PublicInnerClassTest"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    
    @Test
    fun PrivateInnerClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "PrivateInnerClassTest"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }

    @Test
    fun PrimitiveFieldShouldNotBeNullTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "PrimitiveFieldShouldNotBeNull"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
        }
    
    @Test
    fun NestedInnerClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "NestedInnerClassTest"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }

    @Test
    fun FieldAccessExpressionShouldNotBeEliminatedTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "FieldAccessExpressionShouldNotBeEliminated"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }

    @Test
    fun AnonymousInterfaceClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnonymousInterfaceClassTest"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }

    @Test
    fun AnonymousClassTest () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnonymousClassTest"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }

    @Test
    fun AnnotationTest0 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest0"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    @Test
    fun AnnotationTest1 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest1"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    @Test
    fun AnnotationTest2 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest2"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    @Test
    fun AnnotationTest3 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest3"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    @Test
    fun AnnotationTest4 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest4"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    @Test
    fun AnnotationTest5 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest5"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    @Test
    fun AnnotationTest6 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest6"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    @Test
    fun AnnotationTest7 () {
        val directory = File("./src/test/classFiles")
        val outputFile = File("output.txt")
        

        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it.name == "AnnotationTest7"}
            .toList()

        subDirs.forEach { subDir ->
            // Include all the files in the directory
            val files = subDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .toList()
            outputFile.appendText("Processing: ${subDir.path}\n")
            testone(files, subDir.name)
            }
    }
    

}