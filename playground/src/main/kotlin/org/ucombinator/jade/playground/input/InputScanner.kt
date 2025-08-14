package org.ucombinator.jade.playground.input

import java.io.File

/**
 * Configuration for input scanning.
 */
data class InputConfig(
    val inputDir: File = File("input"),
    val fileExtension: String = "java"
)

/**
 * Utility for discovering and filtering Java source files in the input directory.
 */
object InputScanner {
    
    /**
     * Discovers all Java files in the specified input directory.
     *
     * @param config The input configuration.
     * @return A list of Java files found in the input directory.
     */
    fun discoverJavaFiles(config: InputConfig = InputConfig()): List<File> {
        if (!config.inputDir.exists() || !config.inputDir.isDirectory) {
            return emptyList()
        }
        
        return config.inputDir.listFiles { file -> 
            file.isFile && file.extension == config.fileExtension 
        }?.toList() ?: emptyList()
    }
    
    /**
     * Filters discovered Java files by specific file names.
     *
     * @param javaFiles The list of Java files to filter.
     * @param fileNames The specific file names to include (e.g., ["Example.java", "Test.java"]).
     * @return A list of Java files that match the specified names.
     */
    fun filterByFileNames(javaFiles: List<File>, fileNames: List<String>): List<File> {
        val nameSet = fileNames.toSet()
        return javaFiles.filter { it.name in nameSet }
    }
    
    /**
     * Filters discovered Java files by a single file name.
     *
     * @param javaFiles The list of Java files to filter.
     * @param fileName The specific file name to include (e.g., "Example.java").
     * @return A list containing the matching file, or empty if not found.
     */
    fun filterByFileName(javaFiles: List<File>, fileName: String): List<File> {
        return javaFiles.filter { it.name == fileName }
    }
    
    /**
     * Gets a list of available Java file names in the input directory.
     *
     * @param config The input configuration.
     * @return A list of file names (without path) found in the input directory.
     */
    fun getAvailableFileNames(config: InputConfig = InputConfig()): List<String> {
        return discoverJavaFiles(config).map { it.name }
    }
}
