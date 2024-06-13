package org.ucombinator.jade.main

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.int
import org.ucombinator.jade.util.DynamicCallerConverter
import org.ucombinator.jade.util.Log

// TODO: analysis to ensure using only the canonical constructor (helps with
// detecting forward version changes) (as a compiler plugin?)

// TODO: exit code list
// TODO: exit codes
// TODO: description
// TODO: header/footer?
// TODO: aliases, description, defaultValueProvider
// TODO: have build generate documentation
// TODO: throw ProgramResult(statusCode)

/** TODO:doc.
 *
 * @param args TODO:doc
 */
fun main(args: Array<String>): Unit =
  Main().subcommands(
    BuildInfo(),
    Decompile(),
    Compile(),
    Diff(),
    Loggers(),
    DownloadIndex(),
    DownloadMaven(),
    CompletionCommand(),
  ).main(args)

// TODO: optionalValue()
// TODO: varargValues()
//   commandLine.setAbbreviatedOptionsAllowed(true)
//   commandLine.setAbbreviatedSubcommandsAllowed(true)
//   commandLine.setOverwrittenOptionsAllowed(true)
//   showAtFileInUsageHelp = true,
//   showEndOfOptionsDelimiterInUsageHelp = true,

/** TODO:doc. */
class Main : CliktCommand() {
  init {
    versionOption(BuildInformation.version!!, message = { BuildInformation.versionMessage })
    // TODO: color and other formatting in help messages
    // TODO: check
    context { helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true) } }
  }

  /** TODO:doc. */
  data class LogSetting(val name: String, val lvl: Level)

  /** TODO:doc. */
  val log: List<LogSetting> by option(
    metavar = "LEVEL",
    help = """
      Set the logging level where LEVEL is a comma-seperated list of LVL or NAME=LVL.
      LVL is one of (case insensitive): off info warning error debug trace all.
      NAME is a qualified package or class name and is relative to `org.ucombinator.jade` unless prefixed with `.`.
    """.trimIndent()
  ).convert { arg ->
    arg.split(",").map {
      val r = it.split("=", limit = 2)
      when (r.size) {
        1 -> LogSetting("", Level.toLevel(r[0]))
        2 -> LogSetting(r[0], Level.toLevel(r[1]))
        else -> TODO("impossible")
      }
    }
  }.default(listOf())

  /** TODO:doc. */
  val logCallerDepth: Int by option(
    metavar = "DEPTH",
    help = "Number of callers to print after log messages",
  ).int().default(0)

  /** TODO:doc. */
  val ioThreads: Int? by option().int()

  /** TODO:doc. */
  val wait: Boolean by option(
    help = "Wait for input from user before running.  This allows time for a debugger to attach to this process."
  ).flag(
    "--no-wait",
    default = false
  )

  override fun run() {
    if (ioThreads != null) System.setProperty(kotlinx.coroutines.IO_PARALLELISM_PROPERTY_NAME, ioThreads.toString())

    DynamicCallerConverter.depthEnd = logCallerDepth

    for ((name, lvl) in log) {
      // TODO: warn if log exists
      // TODO: warn if no such class or package (and suggest qualifications)
      val parsedName =
        if (name.startsWith(".")) {
          name.substring(1)
        } else if (name == "") {
          ""
        } else {
          "org.ucombinator.jade.${lvl}" // TODO: autodetect or take from BuildInfo
        }
      Log.getLog(parsedName).setLevel(lvl)
    }

    if (wait) {
      // We use the system console in case stdin or stdout are redirected
      System.console().printf("Waiting for user.  Press \"Enter\" to continue.")
      System.console().readLine()
    }
  }
}
