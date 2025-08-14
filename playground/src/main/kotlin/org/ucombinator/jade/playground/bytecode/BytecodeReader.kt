package org.ucombinator.jade.playground.bytecode

import java.io.File
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

/**
 * Utility for reading .class files and converting them to ASM ClassNode structures.
 */
object BytecodeReader {
    
    /**
     * Reads a .class file and converts it to a ClassNode.
     *
     * @param classFile The .class file to read.
     * @return The ClassNode representing the bytecode structure.
     * @throws RuntimeException if the file cannot be read or parsed.
     */
    fun readClassFile(classFile: File): ClassNode {
        require(classFile.exists()) { "Class file does not exist: ${classFile.absolutePath}" }
        require(classFile.extension == "class") { "File must be a .class file: ${classFile.name}" }
        
        return try {
            val classNode = ClassNode()
            ClassReader(classFile.inputStream()).accept(classNode, 0)
            classNode
        } catch (e: Exception) {
            throw RuntimeException("Failed to read class file ${classFile.absolutePath}: ${e.message}", e)
        }
    }
    
    /**
     * Reads multiple .class files and converts them to ClassNode structures.
     *
     * @param classFiles The list of .class files to read.
     * @return A list of ClassNode objects representing the bytecode structures.
     */
    fun readClassFiles(classFiles: List<File>): List<ClassNode> {
        return classFiles.map { readClassFile(it) }
    }
}
