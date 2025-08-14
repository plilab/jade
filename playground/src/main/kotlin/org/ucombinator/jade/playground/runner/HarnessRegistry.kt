package org.ucombinator.jade.playground.runner

import java.io.File
import org.ucombinator.jade.playground.harness.AnalyzerHarness
import org.ucombinator.jade.playground.harness.MethodBodyDecompilationHarness
import org.ucombinator.jade.playground.harness.PlaygroundHarness

/**
 * Factory for creating harness instances bound to a specific input/output context.
 */
interface HarnessFactory {
    val key: String
    val description: String
    fun create(inputFile: File, outputFile: File): PlaygroundHarness
}

/**
 * Registry for managing available playground harness factories.
 */
object HarnessRegistry {
    private val factories: Map<String, HarnessFactory> = listOf(
        object : HarnessFactory {
            override val key: String = AnalyzerHarness.KEY
            override val description: String = AnalyzerHarness.DESCRIPTION
            override fun create(inputFile: File, outputFile: File): PlaygroundHarness =
                AnalyzerHarness(inputFile, outputFile)
        },
        object : HarnessFactory {
            override val key: String = MethodBodyDecompilationHarness.KEY
            override val description: String = MethodBodyDecompilationHarness.DESCRIPTION
            override fun create(inputFile: File, outputFile: File): PlaygroundHarness =
                MethodBodyDecompilationHarness(inputFile, outputFile)
        },
    ).associateBy { it.key }

    fun getAvailableKeys(): Set<String> = factories.keys

    fun getHarnessDescriptions(): Map<String, String> =
        factories.mapValues { it.value.description }

    fun hasHarness(key: String): Boolean = key in factories

    fun getFactory(key: String): HarnessFactory? = factories[key]
}
