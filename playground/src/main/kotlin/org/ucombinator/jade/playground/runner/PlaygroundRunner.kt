package org.ucombinator.jade.playground.runner

import java.io.File
import org.ucombinator.jade.playground.input.InputScanner
import org.ucombinator.jade.playground.input.InputConfig
import org.ucombinator.jade.playground.compiler.JavaSourceCompiler
import org.ucombinator.jade.playground.bytecode.BytecodeReader
import org.ucombinator.jade.playground.bytecode.MethodSelector
 

/**
 * Configuration for the playground runner.
 */
data class PlaygroundConfig(
    val harnessKey: String = "analyzer",
    val javaFileName: String? = null,
    val methodName: String? = null,
    val inputDir: File = File("input"),
    val classesOutputDir: File = File("output/classes")
)

/**
 * Main orchestrator that coordinates input discovery, compilation, bytecode reading, and harness execution.
 */
object PlaygroundRunner {

    /**
     * Runs the playground with the given configuration.
     *
     * @param config The playground configuration.
     */
    fun run(config: PlaygroundConfig) {
        // Validate harness
        val hasKey = HarnessRegistry.hasHarness(config.harnessKey)
        if (!hasKey) {
            println("Unknown harness '${config.harnessKey}'. Available: ${HarnessRegistry.getAvailableKeys()}")
            return
        }

        // Ensure output directory exists
        config.classesOutputDir.mkdirs()

        // Discover input files
        val inputConfig = InputConfig(config.inputDir)
        val allJavaFiles = InputScanner.discoverJavaFiles(inputConfig)
        if (allJavaFiles.isEmpty()) {
            println("No .java files found in ${config.inputDir.absolutePath}")
            return
        }

        // Filter files based on configuration
        val filesToProcess = if (config.javaFileName != null) {
            val filtered = InputScanner.filterByFileName(allJavaFiles, config.javaFileName)
            if (filtered.isEmpty()) {
                println("No file named '${config.javaFileName}' in ${config.inputDir.absolutePath}. Found: ${allJavaFiles.map { it.name }}")
                return
            }
            filtered
        } else {
            allJavaFiles
        }

        // Print header
        println("=".repeat(60))
        val descriptions = HarnessRegistry.getHarnessDescriptions()
        val description = descriptions[config.harnessKey] ?: ""
        println("Playground Harness: ${config.harnessKey} - ${description}")
        println("Method filter: ${config.methodName ?: "<default>"}")
        println("Processing ${filesToProcess.size} file(s)")
        println("=".repeat(60))

        // Process each file
        for (javaFile in filesToProcess) {
            try {
                println("\n--- Processing ${javaFile.name} ---")
                
                // Compile Java file
                val compilationResult = JavaSourceCompiler.compileJavaFile(javaFile, config.classesOutputDir)
                
                // Read bytecode
                val classNode = BytecodeReader.readClassFile(compilationResult.classFile)
                
                // Select method
                val methodSelector = MethodSelector(config.methodName)
                val targetMethod = methodSelector.select(classNode)
                
                if (targetMethod == null) {
                    val availableMethods = methodSelector.getAvailableMethodNames(classNode)
                    println("Method '${config.methodName ?: "main"}' not found in ${classNode.name}. Available: $availableMethods")
                } else {
                    // Prepare output file per input and harness
                    val harness = HarnessRegistry.newHarness(config.harnessKey, javaFile)
                        ?: error("Failed to create harness for key ${config.harnessKey}")

                    // Run harness with streaming output via default print/println
                    harness.run(classNode, targetMethod)
                }
                
                println("--- Finished ${javaFile.name} ---")
            } catch (e: Exception) {
                println("Failed to process ${javaFile.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Lists available harnesses.
     */
    fun listHarnesses() {
        println("Available harnesses:")
        HarnessRegistry.getHarnessDescriptions().forEach { (key, description) ->
            println("  $key: $description")
        }
    }

    /**
     * Lists available input files.
     *
     * @param inputDir The input directory to scan (defaults to "input").
     */
    fun listInputFiles(inputDir: File = File("input")): List<String> {
        val inputConfig = InputConfig(inputDir)
        val files = InputScanner.getAvailableFileNames(inputConfig)
        
        if (files.isEmpty()) {
            println("No .java files found in ${inputDir.absolutePath}")
        } else {
            println("Available input files in ${inputDir.absolutePath}:")
            files.forEach { fileName ->
                println("  $fileName")
            }
        }
        return files
    }
}
