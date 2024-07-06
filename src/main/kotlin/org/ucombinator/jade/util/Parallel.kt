package org.ucombinator.jade.util

import mu.KLogger
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runInterruptible

/** TODO:doc. */
object Parallel { // TODO: rename to concurrency
  var log = Log {}

  /** TODO:doc. */
  var timeout = 5 * 60.0

  /** TODO:doc. */
  private fun time(startTime: Long): String = "%.2f".format((System.nanoTime() - startTime).toDouble() / 1e9)

  // TODO: Move run() to a top level function (and rename)

  fun outputFiles(base: File, passSuffix: String, failSuffix: String): Pair<File, File> =
    Pair(File("${base}${passSuffix}"), File("${base}${failSuffix}"))

  /** TODO:doc. */
  fun <T> finallyOrShutdown(handler: (normalExit: Boolean?) -> Unit, block: (Runtime) -> T): T {
    // TODO: add names to function types: (normalExit: Boolean) -> Unit
    val runtime = Runtime.getRuntime()
    val hook = Thread { handler(null) }
    runtime.addShutdownHook(hook)
    val result = try {
      try {
        block(runtime)
      } finally {
        runtime.removeShutdownHook(hook)
      }
    // } finally {
    //   runtime.removeShutdownHook(thread)
    //   hook(false)
    // }
    } catch (e: Throwable) {
      handler(false)
      throw e
    }
    handler(true)
    return result
  }

  // TODO: could we replace with GNU Parallel or make this work across machines?

  /** TODO:doc. */
  fun <T> run(
    log: KLogger,
    tasks: Collection<T>,
    outputFiles: (T) -> Pair<File, File>,
    isPermanent: (Throwable) -> Boolean, // TODO: flag for retry (can't do 'Boolean?' because need to know when retry limit reached
    // retry: true
    //   permanent: true
    //   permanent: false
    // retry: false
    //   permanent: true
    //   permanent: false
    block: (T) -> ByteArray,
  ) {
    val jobs = object {
      val total = tasks.size
      val waiting = AtomicInteger(total)
      val running = Collections.synchronizedMap(mutableMapOf<T, Long>())
    }
    val passes = object {
      val cached = AtomicInteger()
      val new = AtomicInteger()
    }
    val fails = object {
      val cached = Collections.synchronizedMap(mutableMapOf<String, Long>())
      val new = Collections.synchronizedMap(mutableMapOf<String, Long>())
      val glitched = Collections.synchronizedMap(mutableMapOf<String, Long>())
    }

    fun printStatus(message: String) {
      // We use intermediate variables to ensure the calculations later in this function are consistent
      val jobsTotal = jobs.total
      val jobsWaiting = jobs.waiting.get()
      val jobsRunning = jobs.running.size
      val jobsDone = jobsTotal - jobsWaiting - jobsRunning

      val passesCached = passes.cached.get()
      val passesNew = passes.new.get()
      val passesTotal = passesCached + passesNew

      val failsCached = fails.cached.values.sum()
      val failsNew = fails.new.values.sum()
      val failsGlitched = fails.glitched.values.sum()
      val failsTotal = failsCached + failsNew + failsGlitched

      // TODO: show non-cached completed count
      fun num(n: Number) = "%,d".format(n).replace(',', '_') // TODO: proper locale instead of replace()
      val taskString = "👷jobs ${num(jobsTotal)} (wait ${num(jobsWaiting)}/run ${num(jobsRunning)}/done ${num(jobsDone)})"
      val passString = "✅pass ${num(passesTotal)} (cache ${num(passesCached)}/new ${num(passesNew)})"
      val failString = "❌fail ${num(failsTotal)} (cache ${num(failsCached)}/new ${num(failsNew)}/glitch ${num(failsGlitched)})"

      println("${taskString}${passString}${failString} ${message}")
    }

    // TODO: error messages for exceptions
    finallyOrShutdown(
      { normalExit: Boolean? ->
        // TODO: flush
        println()
        printStatus(when (normalExit) { true -> "✅normal exit"; false -> "❌exception throw"; null -> "🛑JVM shutdown" })

        fun <K, V : Comparable<V>> Map<K, V>.sorted() =
          this.toList().sortedBy { it.first.toString() }.sortedByDescending { it.second }

        fun <K : Comparable<K>, V : Comparable<V>> printFails(header: String, entries: List<Pair<K, V>>) {
          println()
          println(header)
          if (entries.isEmpty()) {
            println("  <none>")
          } else {
            for ((key, value) in entries) println("  $value \t$key")
          }
        }

        printFails("Currently running (seconds):", jobs.running.sorted().map { it.first.toString() to time(it.second) })
        printFails("Fails (Cache):", fails.cached.sorted())
        printFails("Fails (New):", fails.new.sorted())
        printFails("Fails (Glitch):", fails.glitched.sorted())
      }
    ) {
      val nonCachedTasks = tasks.mapNotNull { task ->
        val (passFile, failFile) = outputFiles(task)
        if (passFile.exists()) {
          jobs.waiting.decrementAndGet()
          passes.cached.incrementAndGet()
          null
        } else if (failFile.exists()) {
          jobs.waiting.decrementAndGet()
          fails.cached.merge(failFile.useLines { it.first() }, 1, Long::plus)
          null
        } else {
          Triple(task, passFile, failFile)
        }
      }

      // TODO: factor code into state vs result code
      printStatus("💾cache loaded")
      runBlocking {
        for ((task, passFile, failFile) in nonCachedTasks) {
          // TODO: .version .version.err .deps and .deps.err files .dep.zst
          // TODO: async vs launch
          async(Dispatchers.IO) {
            val startTime = System.nanoTime()
            fun printResult(type: String) { printStatus("${type} ${task} (${time(startTime)} sec)") }
            fun writeResult(file: File, result: ByteArray) {
              try {
                AtomicWriteFile.write(file, result, true)
              } catch (e: Throwable) { // TODO: could this be more specific than Throwable?
                log.error("Failed to write result to file ${file}", e)
              }
            }

            // TODO: always decrement before increment, always print after adjusting numbers, status always last (after errors)
            @Suppress("detekt:TooGenericExceptionCaught")
            try {
              // TODO: NOTE: runInterruptible required to work with non-cooperative
              // TODO: how far out should timeout be?
              // TODO: track and print the number of timeouts
              // 48	java.io.UncheckedIOException:java.nio.channels.FileLockInterruptionException
              // 36	java.lang.RuntimeException:java.lang.RuntimeException:java.lang.InterruptedException
              jobs.waiting.decrementAndGet()
              jobs.running.put(task, startTime)
              printStatus("🏃start  ${task}")
              try {
                writeResult(passFile, withTimeout(timeout.seconds) { runInterruptible { block(task) } })
              } finally {
                jobs.running.remove(task)
              }
              passes.new.incrementAndGet()
              printResult("✅passed")
            } catch (e: Throwable) {
              val name = Exceptions.name(e)
              if (!isPermanent(e)) {
                fails.glitched.merge(name, 1, Long::plus)
                log.error(name, e)
                printResult("🚧glitch")
              } else {
                fails.new.merge(name, 1, Long::plus)
                writeResult(failFile, "${name}\n--------\n${e.stackTraceToString()}".toByteArray())
                printResult("❌failed")
              }
            }
          }
        }
      }
    }
  }
}
