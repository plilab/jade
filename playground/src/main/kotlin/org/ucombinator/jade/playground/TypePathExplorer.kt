package org.ucombinator.jade.playground

import org.objectweb.asm.TypePath

/**
 * Utility object for exploring and demonstrating ASM TypePath functionality.
 * TypePath represents paths to navigate within complex generic types in bytecode analysis.
 */
object TypePathExplorer {

    /**
     * Runs a comprehensive exploration of ASM TypePath functionality,
     * demonstrating various path types and their usage.
     */
    fun exploreTypePaths() {
        println("\nWhat is TypePath?")
        println("TypePath represents a path to navigate within complex generic types.")
        println("It's used in bytecode analysis to pinpoint specific type locations.")
        
        demonstrateArrayElementPaths()
        demonstrateInnerTypePaths()
        demonstrateTypeArgumentPaths()
        demonstrateWildcardBoundPaths()
        demonstrateComplexNestedPaths()
        explainPracticalUseCases()
        demonstrateTypePathInspection()
    }

    /**
     * Demonstrates array element type paths.
     * Notation: '[' represents stepping into array element type.
     */
    private fun demonstrateArrayElementPaths() {
        println("\n1. ARRAY ELEMENT TYPE PATHS")
        println("   Notation: '[' represents stepping into array element type")
        
        val arrayPath1 = TypePath.fromString("[")  // int[] -> int
        val arrayPath2 = TypePath.fromString("[[")  // int[][] -> int[] -> int
        
        println("   '[' path: ${describeTypePath(arrayPath1)} (for types like: String[] -> String)")
        println("   '[[' path: ${describeTypePath(arrayPath2)} (for types like: String[][] -> String[] -> String)")
    }

    /**
     * Demonstrates inner/nested type paths.
     * Notation: '.' represents stepping into inner/nested types.
     */
    private fun demonstrateInnerTypePaths() {
        println("\n2. INNER TYPE PATHS")
        println("   Notation: '.' represents stepping into inner/nested types")
        
        val innerPath = TypePath.fromString(".")  // Outer.Inner
        println("   '.' path: ${describeTypePath(innerPath)} (for types like: Outer.Inner)")
    }

    /**
     * Demonstrates type argument paths.
     * Notation: number + ';' represents stepping into type arguments.
     */
    private fun demonstrateTypeArgumentPaths() {
        println("\n3. TYPE ARGUMENT PATHS")
        println("   Notation: number + ';' represents stepping into type arguments")
        
        val typeArgPath1 = TypePath.fromString("0;")  // List<String> -> String (first type arg)
        val typeArgPath2 = TypePath.fromString("1;")  // Map<String, Integer> -> Integer (second type arg)
        
        println("   '0;' path: ${describeTypePath(typeArgPath1)} (for types like: List<String> -> String)")
        println("   '1;' path: ${describeTypePath(typeArgPath2)} (for types like: Map<String, Integer> -> Integer)")
    }

    /**
     * Demonstrates wildcard bound paths.
     * Notation: '*' represents stepping into wildcard bounds.
     */
    private fun demonstrateWildcardBoundPaths() {
        println("\n4. WILDCARD BOUND PATHS")
        println("   Notation: '*' represents stepping into wildcard bounds")
        
        val wildcardPath = TypePath.fromString("*")  // ? extends String -> String
        println("   '*' path: ${describeTypePath(wildcardPath)} (for types like: ? extends String -> String)")
    }

    /**
     * Demonstrates complex nested type paths combining multiple navigation types.
     */
    private fun demonstrateComplexNestedPaths() {
        println("\n5. COMPLEX NESTED PATHS")
        println("   You can combine these to navigate deep type structures!")
        
        val complexPath1 = TypePath.fromString("0;[")  // List<String[]> -> String[] -> String
        val complexPath2 = TypePath.fromString("0;0;")  // List<Map<String, Integer>> -> Map<String, Integer> -> String
        val complexPath3 = TypePath.fromString("[0;")  // List<String>[] -> List<String> -> String
        
        println("   '0;[' path: ${describeTypePath(complexPath1)}")
        println("     (for: List<String[]> -> String[] -> String)")
        println("   '0;0;' path: ${describeTypePath(complexPath2)}")
        println("     (for: List<Map<String, Integer>> -> Map<String, Integer> -> String)")
        println("   '[0;' path: ${describeTypePath(complexPath3)}")
        println("     (for: List<String>[] -> List<String> -> String)")
    }

    /**
     * Explains practical use cases for TypePath in ASM.
     */
    private fun explainPracticalUseCases() {
        println("\n6. PRACTICAL USE CASES")
        println("   TypePath is used in ASM for:")
        println("   - Type annotations on specific parts of generic types")
        println("   - Analyzing complex generic type structures in bytecode")
        println("   - Navigating to specific type locations for transformations")
        println("   - Understanding where type information is located in signatures")
    }

    /**
     * Demonstrates inspection of TypePath properties and structure.
     */
    private fun demonstrateTypePathInspection() {
        println("\n7. INSPECTING TYPEPATH PROPERTIES")
        val examplePath = TypePath.fromString("0;[1;")
        if (examplePath != null) {
            println("   Path '0;[1;' analysis:")
            println("     Length: ${examplePath.length} steps")
            for (i in 0 until examplePath.length) {
                val step = examplePath.getStep(i)
                val stepName = when (step) {
                    TypePath.ARRAY_ELEMENT -> "ARRAY_ELEMENT"
                    TypePath.INNER_TYPE -> "INNER_TYPE"
                    TypePath.TYPE_ARGUMENT -> "TYPE_ARGUMENT"
                    TypePath.WILDCARD_BOUND -> "WILDCARD_BOUND"
                    else -> "UNKNOWN"
                }
                
                val argument = if (step == TypePath.TYPE_ARGUMENT) {
                    " (arg index: ${examplePath.getStepArgument(i)})"
                } else ""
                
                println("     Step $i: $stepName$argument")
            }
            println("     This would navigate: Generic<Type>[] -> Generic<Type> -> second type arg of Generic")
        }
    }

    /**
     * Describes a TypePath in human-readable format.
     *
     * @param typePath The TypePath to describe (can be null).
     * @return A human-readable description of the path.
     */
    fun describeTypePath(typePath: TypePath?): String {
        if (typePath == null) return "null (empty path)"
        
        if (typePath.length == 0) return "empty path"
        
        val steps = mutableListOf<String>()
        for (i in 0 until typePath.length) {
            val step = typePath.getStep(i)
            val stepDescription = when (step) {
                TypePath.ARRAY_ELEMENT -> "go to array element"
                TypePath.INNER_TYPE -> "go to inner type"
                TypePath.TYPE_ARGUMENT -> "go to type argument ${typePath.getStepArgument(i)}"
                TypePath.WILDCARD_BOUND -> "go to wildcard bound"
                else -> "unknown step"
            }
            steps.add(stepDescription)
        }
        
        return "${typePath.length} step(s): ${steps.joinToString(" -> ")}"
    }
} 