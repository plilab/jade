package org.ucombinator.jade.playground

import org.ucombinator.jade.util.ReadFiles
import java.io.ByteArrayInputStream
import java.io.BufferedInputStream
import org.objectweb.asm.TypePath
import org.ucombinator.jade.playground.compileTest3

fun main() {

    // ==========================
    // Test ReadFiles
    // ==========================
    println("\n" + "=".repeat(60))
    println("Test ReadFiles")
    println("=".repeat(60))

    val readFiles = ReadFiles()
    val classFileSignature = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
    val notClassFileSignature = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())

    // The function requires a stream that supports mark/reset.
    val correctInputStream = BufferedInputStream(ByteArrayInputStream(classFileSignature))
    val incorrectInputStream = BufferedInputStream(ByteArrayInputStream(notClassFileSignature))

    val result1 = readFiles.headerMatches(correctInputStream, classFileSignature.toList())
    val result2 = readFiles.headerMatches(incorrectInputStream, classFileSignature.toList())

    println("Testing with correct Java class file signature (CAFEBABE):")
    println("Expected: true, Got: $result1")

    println("Testing with incorrect signature (DEADBEEF):")
    println("Expected: false, Got: $result2")

    if (result1 && !result2) {
        println("\nTest successful! The playground is correctly set up.")
    } else {
        println("\nTest failed. Something is wrong with the playground setup.")
    }

    // ==========================
    // Explore ASM TypePath!
    // ==========================
    println("\n" + "=".repeat(60))
    println("EXPLORING ASM TYPEPATH")
    println("=".repeat(60))
    
    exploreTypePaths()

    // ==========================
    // Explore Compile
    // ==========================
    println("\n" + "=".repeat(60))
    println("Explore Compile")
    println("=".repeat(60))

    val test3 = compileTest3()
    println("Test3 compiled to: ${test3.absolutePath}")

    // ==========================
    // Explore Jade Decompile
    // ==========================
    println("\n" + "=".repeat(60))
    println("Explore Jade Decompile")
    println("=".repeat(60))

    decompileFile(test3)
}

fun exploreTypePaths() {
    println("\nWhat is TypePath?")
    println("TypePath represents a path to navigate within complex generic types.")
    println("It's used in bytecode analysis to pinpoint specific type locations.")
    
    println("\n1. ARRAY ELEMENT TYPE PATHS")
    println("   Notation: '[' represents stepping into array element type")
    
    // Array type paths - stepping into array element types
    val arrayPath1 = TypePath.fromString("[")  // int[] -> int
    val arrayPath2 = TypePath.fromString("[[")  // int[][] -> int[] -> int
    
    println("   '[' path: ${describeTypePath(arrayPath1)} (for types like: String[] -> String)")
    println("   '[[' path: ${describeTypePath(arrayPath2)} (for types like: String[][] -> String[] -> String)")
    
    println("\n2. INNER TYPE PATHS")
    println("   Notation: '.' represents stepping into inner/nested types")
    
    // Inner type paths - stepping into nested types
    val innerPath = TypePath.fromString(".")  // Outer.Inner
    println("   '.' path: ${describeTypePath(innerPath)} (for types like: Outer.Inner)")
    
    println("\n3. TYPE ARGUMENT PATHS")
    println("   Notation: number + ';' represents stepping into type arguments")
    
    // Type argument paths - stepping into generic type arguments
    val typeArgPath1 = TypePath.fromString("0;")  // List<String> -> String (first type arg)
    val typeArgPath2 = TypePath.fromString("1;")  // Map<String, Integer> -> Integer (second type arg)
    
    println("   '0;' path: ${describeTypePath(typeArgPath1)} (for types like: List<String> -> String)")
    println("   '1;' path: ${describeTypePath(typeArgPath2)} (for types like: Map<String, Integer> -> Integer)")
    
    println("\n4. WILDCARD BOUND PATHS")
    println("   Notation: '*' represents stepping into wildcard bounds")
    
    // Wildcard bound paths - stepping into bounds of wildcards
    val wildcardPath = TypePath.fromString("*")  // ? extends String -> String
    println("   '*' path: ${describeTypePath(wildcardPath)} (for types like: ? extends String -> String)")
    
    println("\n5. COMPLEX NESTED PATHS")
    println("   You can combine these to navigate deep type structures!")
    
    // Complex combinations
    val complexPath1 = TypePath.fromString("0;[")  // List<String[]> -> String[] -> String
    val complexPath2 = TypePath.fromString("0;0;")  // List<Map<String, Integer>> -> Map<String, Integer> -> String
    val complexPath3 = TypePath.fromString("[0;")  // List<String>[] -> List<String> -> String
    
    println("   '0;[' path: ${describeTypePath(complexPath1)}")
    println("     (for: List<String[]> -> String[] -> String)")
    println("   '0;0;' path: ${describeTypePath(complexPath2)}")
    println("     (for: List<Map<String, Integer>> -> Map<String, Integer> -> String)")
    println("   '[0;' path: ${describeTypePath(complexPath3)}")
    println("     (for: List<String>[] -> List<String> -> String)")
    
    println("\n6. PRACTICAL USE CASES")
    println("   TypePath is used in ASM for:")
    println("   - Type annotations on specific parts of generic types")
    println("   - Analyzing complex generic type structures in bytecode")
    println("   - Navigating to specific type locations for transformations")
    println("   - Understanding where type information is located in signatures")
    
    // Demonstrate inspection of TypePath properties
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