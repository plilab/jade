package org.ucombinator.jade.playground

import org.ucombinator.jade.util.ReadFiles
import java.io.ByteArrayInputStream
import java.io.BufferedInputStream

fun main() {
    println("Hello from the playground!")
    println("Let's test a function from the main project.")

    // Let's test the `headerMatches` function from `ReadFiles.kt`.
    // It checks if an input stream starts with a specific byte sequence.
    // The Java class file signature is 0xCAFEBABE.

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
} 