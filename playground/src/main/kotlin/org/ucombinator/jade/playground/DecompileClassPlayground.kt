package org.ucombinator.jade.playground

import org.ucombinator.jade.decompile.Decompile
import java.io.File

fun decompileFile(file: File) {
    val outputDir = File("decompile-output")
    outputDir.mkdirs() // Ensure the output directory exists

    Decompile.main(listOf(file), outputDir)
}
