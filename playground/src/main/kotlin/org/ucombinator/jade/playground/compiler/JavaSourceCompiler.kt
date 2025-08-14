package org.ucombinator.jade.playground.compiler

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.ToolProvider
import org.ucombinator.jade.util.Log

/**
 * Represents the result of compiling a Java source file.
 */
data class CompilationResult(
    val classFile: File,
    val className: String
)

/**
 * Utility object for compiling Java source files.
 */
object JavaSourceCompiler {
    private val log = Log {}

    /**
     * Compiles a Java source file into a .class file.
     *
     * @param javaFile The Java source file to compile.
     * @param outputDir The directory where the compiled .class file should be placed.
     * @return The compilation result containing the compiled .class file and class name.
     * @throws RuntimeException if compilation fails or the compiler is not available.
     */
    fun compileJavaFile(javaFile: File, outputDir: File): CompilationResult {
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
            return CompilationResult(classFile, className)

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
     * @return The compilation result containing the compiled .class file and class name.
     */
    fun compileJavaSource(
        javaSource: String,
        className: String,
        tempDir: File? = null,
        keepIntermediateFiles: Boolean = false
    ): CompilationResult {
        val workingDir = tempDir ?: Files.createTempDirectory("jade_compilation").toFile()
        workingDir.mkdirs() // Ensure directory exists

        // Create temporary Java file
        val javaFile = File(workingDir, "$className.java")
        javaFile.writeText(javaSource)

        var result: CompilationResult? = null
        try {
            result = compileJavaFile(javaFile, workingDir)
            return result
        } finally {
            if (!keepIntermediateFiles) {
                javaFile.delete() // Always delete the temp .java file
                result?.classFile?.delete() // Delete the compiled .class file if it was created
                if (tempDir == null) { // Only delete the working directory if we created it
                    workingDir.deleteRecursively()
                }
            }
        }
    }

    /**
     * Compiles multiple Java source files.
     *
     * @param javaFiles The Java source files to compile.
     * @param outputDir The directory where the compiled .class files should be placed.
     * @return A list of compilation results for each successfully compiled file.
     */
    fun compileJavaFiles(javaFiles: List<File>, outputDir: File): List<CompilationResult> {
        return javaFiles.map { javaFile ->
            compileJavaFile(javaFile, outputDir)
        }
    }
}
