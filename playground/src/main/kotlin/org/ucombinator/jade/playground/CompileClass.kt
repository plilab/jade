package org.ucombinator.jade.playground

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.ToolProvider
import org.ucombinator.jade.util.Log

/**
 * Utility object for compiling Java source files.
 */
object JavaCompiler {
    private val log = Log {}

    /**
     * Compiles a Java source file into a .class file.
     *
     * @param javaFile The Java source file to compile.
     * @param outputDir The directory where the compiled .class file should be placed.
     * @return The compiled .class file.
     * @throws RuntimeException if compilation fails or the compiler is not available.
     */
    fun compileJavaFile(javaFile: File, outputDir: File): File {
        require(javaFile.exists()) { "Java file does not exist: ${javaFile.absolutePath}" }
        require(javaFile.extension == "java") { "File must be a .java file: ${javaFile.name}" }
        require(outputDir.isDirectory || outputDir.mkdirs()) { "Output directory could not be created: ${outputDir.absolutePath}" }

        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: throw RuntimeException("No Java compiler available. Make sure you're running on a JDK, not JRE.")

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val standardFileManager = compiler.getStandardFileManager(diagnostics, null, null)

        try {
            val compilationUnits = standardFileManager.getJavaFileObjectsFromFiles(listOf(javaFile))
            // Include current classpath to resolve dependencies if any
            val options = listOf("-d", outputDir.absolutePath, "-cp", System.getProperty("java.class.path"))

            log.info { "Compiling ${javaFile.name} to ${outputDir.absolutePath}..." }
            val task = compiler.getTask(null, standardFileManager, diagnostics, options, null, compilationUnits)
            val success = task.call()

            if (!success) {
                val errors = diagnostics.diagnostics.joinToString("\n") { diagnostic ->
                    "Line ${diagnostic.lineNumber}: ${diagnostic.getMessage(null)}"
                }
                throw RuntimeException("Compilation failed for ${javaFile.name}:\n$errors")
            }

            val className = javaFile.nameWithoutExtension
            val classFile = File(outputDir, "$className.class")

            if (!classFile.exists()) {
                throw RuntimeException("Expected class file not found after successful compilation: ${classFile.absolutePath}")
            }
            log.info { "Successfully compiled ${javaFile.name} to ${classFile.absolutePath}" }
            return classFile

        } finally {
            standardFileManager.close()
        }
    }

    /**
     * Compiles Java source code from a string into a .class file.
     *
     * @param javaSource The Java source code as a string.
     * @param className The name of the class defined in the source (e.g., "MyClass").
     * @param tempDir Optional temporary directory for intermediate files (auto-created if null).
     * @param keepIntermediateFiles Whether to keep .java and .class files after compilation (default: false).
     * @return The compiled .class file.
     */
    fun compileJavaSource(
        javaSource: String,
        className: String,
        tempDir: File? = null,
        keepIntermediateFiles: Boolean = false
    ): File {
        val workingDir = tempDir ?: Files.createTempDirectory("jade_compilation").toFile()
        workingDir.mkdirs() // Ensure directory exists

        // Create temporary Java file
        val javaFile = File(workingDir, "$className.java")
        javaFile.writeText(javaSource)

        var compiledFile: File? = null
        try {
            compiledFile = compileJavaFile(javaFile, workingDir)
            return compiledFile
        } finally {
            if (!keepIntermediateFiles) {
                javaFile.delete() // Always delete the temp .java file
                compiledFile?.delete() // Delete the compiled .class file if it was created
                if (tempDir == null) { // Only delete the working directory if we created it
                    workingDir.deleteRecursively()
                }
            }
        }
    }
}

/**
 * Convenience function to compile Test3.java specifically.
 * The compiled .class file will be placed in "playground/compiled_classes".
 *
 * @return The compiled .class file.
 */
fun compileTest3(): File {
    val test3File = File("input/Test3.java")
    val outputDir = File("output")
    outputDir.mkdirs() // Ensure the output directory exists
    return JavaCompiler.compileJavaFile(test3File, outputDir)
}
