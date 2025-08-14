package org.ucombinator.jade.playground.cli

import java.io.File
import org.ucombinator.jade.playground.runner.PlaygroundRunner
import org.ucombinator.jade.playground.runner.PlaygroundConfig
import org.ucombinator.jade.playground.runner.HarnessRegistry

/**
 * Main entry point for the playground CLI.
 */
fun main() {
    println("\n--- Jade Playground: Running all harnesses on all inputs ---")
    val harnessKeys = HarnessRegistry.getAvailableKeys()
    if (harnessKeys.isEmpty()) {
        println("No harnesses registered.")
        return
    }
    for (key in harnessKeys) {
        val config = PlaygroundConfig(
            harnessKey = key,
            inputDir = File("input"),
            classesOutputDir = File("output/classes")
        )
        PlaygroundRunner.run(config)
    }
    println("--- Completed running all harnesses ---")
}
