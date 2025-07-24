package org.ucombinator.jade.playground

import java.io.File

fun main() {
    println("=".repeat(60))
    println("Decompiling method bodies from playground/input")
    println("=".repeat(60))

    val inputDir = File("input")
    val outputDir = File("out/playground_compiled")
    outputDir.mkdirs()

    val javaFiles = inputDir.listFiles { file -> file.extension == "java" && file.isFile }

    if (javaFiles.isNullOrEmpty()) {
        println("No .java files found in ${inputDir.absolutePath}")
        return
    }

    println("Found ${javaFiles.size} java files to process.")

    javaFiles.forEach { javaFile ->
        try {
            println("\n--- Processing ${javaFile.name} ---")
            val classFile = JavaCompiler.compileJavaFile(javaFile, outputDir)
            testMethodBodyDecompilation(classFile)
            println("--- Finished processing ${javaFile.name} ---")
        } catch (e: Exception) {
            println("Failed to process ${javaFile.name}: ${e.message}")
        }
    }
}