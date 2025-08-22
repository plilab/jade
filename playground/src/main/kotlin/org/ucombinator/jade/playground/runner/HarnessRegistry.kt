package org.ucombinator.jade.playground.runner

import java.io.File
import org.ucombinator.jade.playground.harness.PlaygroundHarness
import org.ucombinator.jade.playground.harness.AnalyzerHarness
import org.ucombinator.jade.playground.harness.DecompileHarness
import org.ucombinator.jade.playground.harness.SSATestHarness
import org.ucombinator.jade.playground.harness.VisualizeDecompileHarness

/**
 * Registry for managing available playground harnesses.
 */
object HarnessRegistry {
    private data class Entry(
        val key: String,
        val description: String,
        val constructor: () -> PlaygroundHarness,
    )

    private val entries: Map<String, Entry> = listOf(
        Entry(AnalyzerHarness.KEY, AnalyzerHarness.DESCRIPTION) { AnalyzerHarness() },
        Entry(DecompileHarness.KEY, DecompileHarness.DESCRIPTION) { DecompileHarness() },
        Entry(SSATestHarness.KEY, SSATestHarness.DESCRIPTION) { SSATestHarness() },
        Entry(VisualizeDecompileHarness.KEY, VisualizeDecompileHarness.DESCRIPTION) { VisualizeDecompileHarness() },
    ).associateBy { it.key }

    fun getAvailableKeys(): Set<String> = entries.keys

    fun getHarnessDescriptions(): Map<String, String> =
        entries.mapValues { it.value.description }

    fun hasHarness(key: String): Boolean = key in entries

    fun newHarness(key: String, inputFile: File): PlaygroundHarness? =
        entries[key]?.constructor?.invoke()?.withInput(inputFile)
}
