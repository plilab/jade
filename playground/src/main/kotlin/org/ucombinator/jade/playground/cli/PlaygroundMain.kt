package org.ucombinator.jade.playground.cli

import java.io.File
import org.ucombinator.jade.playground.runner.PlaygroundRunner
import org.ucombinator.jade.playground.runner.PlaygroundConfig
import org.ucombinator.jade.playground.runner.HarnessRegistry

fun selectHarnesses(harnessKeysList: List<String>, selectedHarnessIndexes: List<Int>): List<Int> {
    println("=====================")
    println("List of available harnesses:")
    for (i in harnessKeysList.indices) {
        val harness = harnessKeysList[i]
        val selected = selectedHarnessIndexes.contains(i)
        println("${i}. ${harness} ${if (selected) "[X]" else "[ ]"}")
    }
    println("Type in \"all\" to run all harnesses.")
    println("Press \"Enter\" to run selected harnesses.")
    print("Enter space-separated list of harness indexes: ")
    val input = readLine()
    when (input) {
        "" -> return selectedHarnessIndexes
        "all" -> return harnessKeysList.indices.toList()
        else -> {
            try {
                val newSelectedIndexes = input?.split(" ")?.map { it.toInt() }?.filter { it in 0 until harnessKeysList.size } ?: listOf()
                val updatedSelectedIndexes = selectedHarnessIndexes.toMutableList()
                newSelectedIndexes.forEach { index ->
                    if (updatedSelectedIndexes.contains(index)) {
                        updatedSelectedIndexes.remove(index)
                    } else {
                        updatedSelectedIndexes.add(index)
                    }
                }
                return selectHarnesses(harnessKeysList, updatedSelectedIndexes)
            } catch (e: NumberFormatException) {
                println("Invalid input. Please enter a list of integers separated by spaces.")
                return selectHarnesses(harnessKeysList, selectedHarnessIndexes)
            }
        }
    }
}

/**
 * Main entry point for the playground CLI.
 */
fun main() {
    val harnessKeys = HarnessRegistry.getAvailableKeys()
    if (harnessKeys.isEmpty()) {
        println("No harnesses registered.")
        return
    }
    val harnessKeysList = harnessKeys.toList()

    // Let user select which harnesses to run
    val selectedHarnessIndexes = selectHarnesses(harnessKeysList, listOf())

    // Run the selected harnesses
    for (index in selectedHarnessIndexes) {
        val key = harnessKeysList[index]
        val config = PlaygroundConfig(
            harnessKey = key,
            inputDir = File("input"),
            classesOutputDir = File("output/classes")
        )
        PlaygroundRunner.run(config)
    }
    println("--- Completed running all harnesses ---")
}
