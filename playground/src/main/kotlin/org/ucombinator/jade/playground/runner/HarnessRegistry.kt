package org.ucombinator.jade.playground.runner

import java.io.File
import org.ucombinator.jade.playground.harness.*

/**
 * Registry for managing available playground harnesses.
 */
object HarnessRegistry {
    private val harnesses: List<PlaygroundHarness> = listOf(
        AnalyzerHarness(),
        DecompileHarness(),
        SSADotHarness(),
        DotHarness(),
        CFGDotHarness(),
    )

    private data class Entry(
        val key: String,
        val description: String,
        val constructor: () -> PlaygroundHarness,
    )

    private val entries: Map<String, Entry> = harnesses.map { harness ->
        Entry(harness.key, harness.description) { harness::class.java.getDeclaredConstructor().newInstance() }
    }.associateBy { it.key }

    fun getAvailableKeys(): Set<String> = entries.keys

    fun getHarnessDescriptions(): Map<String, String> =
        entries.mapValues { it.value.description }

    fun hasHarness(key: String): Boolean = key in entries

    fun newHarness(key: String, inputFile: File): PlaygroundHarness? =
        entries[key]?.constructor?.invoke()?.withInput(inputFile)
}
