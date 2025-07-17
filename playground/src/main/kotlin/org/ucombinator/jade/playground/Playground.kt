package org.ucombinator.jade.playground

import org.ucombinator.jade.util.ReadFiles
import java.io.ByteArrayInputStream
import java.io.BufferedInputStream
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
    
    TypePathExplorer.exploreTypePaths()

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
