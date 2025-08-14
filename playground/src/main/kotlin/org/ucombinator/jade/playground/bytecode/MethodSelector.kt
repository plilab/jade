package org.ucombinator.jade.playground.bytecode

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * Strategy for selecting methods from a ClassNode.
 */
data class MethodSelector(
    val name: String? = null,
    val preferMainIfMissing: Boolean = true
) {
    
    /**
     * Selects a method from the given ClassNode based on the selection strategy.
     *
     * @param classNode The ClassNode to select a method from.
     * @return The selected MethodNode, or null if no suitable method is found.
     */
    fun select(classNode: ClassNode): MethodNode? {
        return when {
            name != null -> classNode.methods.find { it.name == name }
            preferMainIfMissing -> classNode.methods.find { it.name == "main" } 
                ?: classNode.methods.firstOrNull()
            else -> classNode.methods.firstOrNull()
        }
    }
    
    /**
     * Gets available method names from the ClassNode.
     *
     * @param classNode The ClassNode to get method names from.
     * @return A list of method names available in the class.
     */
    fun getAvailableMethodNames(classNode: ClassNode): List<String> {
        return classNode.methods.map { it.name }
    }
}

/**
 * Result of method selection containing both the selected method and context.
 */
data class MethodSelectionResult(
    val methodNode: MethodNode?,
    val availableMethods: List<String>,
    val wasFound: Boolean
) {
    companion object {
        fun found(methodNode: MethodNode, availableMethods: List<String>) = 
            MethodSelectionResult(methodNode, availableMethods, true)
            
        fun notFound(availableMethods: List<String>) = 
            MethodSelectionResult(null, availableMethods, false)
    }
}
