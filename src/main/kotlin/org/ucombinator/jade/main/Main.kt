package org.ucombinator.jade.main

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.Context
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
import java.io.File

// TODO: analysis to ensure using only the canonical constructor (helps with detecting forward version changes) (as a
//   compiler plugin?)
// TODO: exit code list
// TODO: exit codes
// TODO: description
// TODO: header/footer?
// TODO: aliases, description, defaultValueProvider
// TODO: have build generate documentation
// TODO: throw ProgramResult(statusCode)
// TODO: show default on boolean flags

/** TODO:doc.
 *
 * @param args TODO:doc
 */
fun main(args: Array<String>) {
  Jade().subcommands(
    Decompile(),
    Compile(),
    Diff(),
    Maven().subcommands(
      Maven.Mirrors(),
      Maven.Index(),
      Maven.IndexToJson(),
      Maven.Versions(),
      Maven.Dependencies(),
      Maven.Download(),
      Maven.ClearLocks(),
    ),
    About().subcommands(
      About.BuildInfo(),
      About.Loggers(),
      CompletionCommand(),
    ),
  ).main(args)
}

// TODO: optionalValue()
// TODO: varargValues()
//   commandLine.setAbbreviatedOptionsAllowed(true)
//   commandLine.setAbbreviatedSubcommandsAllowed(true)
//   commandLine.setOverwrittenOptionsAllowed(true)
//   showAtFileInUsageHelp = true,
//   showEndOfOptionsDelimiterInUsageHelp = true,

/** TODO: doc. */
abstract class JadeCommand() : CliktCommand() {
  init {
    // TODO: color and other formatting in help messages
    // TODO: better terminal colors for `code`
    // TODO: check
    installMordantMarkdown() // TODO: versus helpFormatter
    context {
      // TODO: helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true) }
      readArgumentFile = { File(it).readText() } // The Clikt default fails on pipes, redirects and devices
    }
  }
}

/** TODO: doc. */
open class NoOpJadeCommand() : JadeCommand() {
  final override fun run() { /* do nothing */ }
}

/** TODO:doc. */
class Jade : JadeCommand() {
  init {
    versionOption(BuildInformation.version!!, message = { BuildInformation.versionMessage })
  }

  /** TODO:doc. */
  val log: List<Pair<String, Level>> by option(
    metavar = "LEVEL",
    help = """
      Set the logging level where LEVEL is a comma-seperated list of LVL or NAME=LVL.
      LVL is one of (case insensitive): off info warning error debug trace all.
      NAME is a qualified package or class name and is relative to `org.ucombinator.jade` unless prefixed with `.`.
    """.trimIndent(),
  ).convert { arg ->
    val r = arg.split("=", limit = 2)
    when (r.size) {
      1 -> "" to Level.toLevel(r[0])
      2 -> r[0] to Level.toLevel(r[1])
      else -> TODO("impossible")
    }
  }.split(Regex(","))
  .default(listOf())

  /** TODO:doc. */
  val logCallerDepth: Int by option(
    metavar = "DEPTH",
    help = "Number of callers to print after log messages",
  ).int().default(DynamicCallerConverter.depthEnd)

  /** TODO:doc. */
  val ioThreads: Int? by option().int()

  /** TODO:doc. */
  val wait: Boolean by option(
    help = "Wait for input from user before running.  This allows time for a debugger to attach to this process.",
  ).flag(
    "--no-wait", // TODO: automate "off" names
    default = false,
  )

  // override fun aliases(): Map<String, List<String>> = mapOf(
  //   "mvn" to listOf("maven"),
  // )

  // TODO: command aliases for all command prefixes
  override fun run() {
    DynamicCallerConverter.depthEnd = logCallerDepth

    ioThreads?.let { System.setProperty(kotlinx.coroutines.IO_PARALLELISM_PROPERTY_NAME, it.toString()) }

    for ((name, level) in log) {
      // TODO: warn if log exists
      // TODO: warn if no such class or package (and suggest qualifications)
      val parsedName = when {
        name.startsWith(".") -> name.substring(1)
        name == "" -> ""
        else -> "org.ucombinator.jade.${level}" // TODO: autodetect or take from BuildInfo
      }
      Log.getLog(parsedName).setLevel(level)
    }

    if (wait) {
      // TODO: use Clikt prompt
      // We use the system console in case stdin or stdout are redirected
      System.console().printf("Waiting for user.  Press \"Enter\" to continue.")
      System.console().readLine()
    }
  }
}
