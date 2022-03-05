package org.ucombinator.jade.main

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import org.ucombinator.jade.util.DynamicCallerConverter
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Vfs
import java.io.File

// import mu.KotlinLogging
// import org.ucombinator.jade.main.BuildInformation

// TODO: analysis to ensure using only the canonical constructor (helps with
// detecting forward version changes) (as a compiler plugin?)

// TODO: exit code list
// TODO: exit codes

// /////////////////////////////////////////////////////////////
// Top-level command

// TODO: description
// TODO: header/footer?
// TODO: aliases, description, defaultValueProvider
// TODO: have build generate documentation

fun main(args: Array<String>): Unit =
  Jade().subcommands(
    TestLog(),
    BuildInfo(),
    Decompile(),
    Compile(),
    Diff(),
    Logs(),
    CompletionCommand(),
    // classOf[ManPageGenerator],
  ).main(args)

//   commandLine.setAbbreviatedOptionsAllowed(true)
//   commandLine.setAbbreviatedSubcommandsAllowed(true)
//   commandLine.setOverwrittenOptionsAllowed(true)
//   showAtFileInUsageHelp = true,
//   showEndOfOptionsDelimiterInUsageHelp = true,
class Jade : CliktCommand() {
  init {
    versionOption(BuildInformation.version!!, message = { BuildInformation.versionMessage })
    context {
      helpFormatter = CliktHelpFormatter(
        showRequiredTag = true,
        showDefaultValues = true,
      )
    }
  }

  data class LogSetting(val name: String, val lvl: Level)
  val log: List<LogSetting> by option(
    metavar = "LEVEL",
    help = """
      Set the logging level where LEVEL is a comma-seperated list of LVL or NAME=LVL.
      LVL is one of (case insensitive): off info warning error debug trace all.
      NAME is a qualified package or class name and is relative to `org.ucombinator.jade` unless prefixed with `.`.
    """
  ).convert {
    it.split(",").map {
      val r = it.split("=", limit = 2)
      when (r.size) {
        1 -> LogSetting("", Level.toLevel(r.get(0)))
        2 -> LogSetting(r.get(0), Level.toLevel(r.get(1)))
        else -> TODO("impossible")
      }
    }
  }.default(listOf())
//     showDefaultValue = CommandLine.Help.Visibility.NEVER,

  val logCallerDepth: Int by option(
    metavar = "DEPTH",
    help = "Number of callers to print after log messages",
  ).int().default(0)

  val wait: Boolean by option(
    help = "Wait for input from user before running.  This allows time for a debugger to attach to this process."
  ).flag(
    "--no-wait",
    default = false
  )

  override fun run() {
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
          Log.PREFIX + lvl
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

// /////////////////////////////////////////////////////////////
// Sub-commands

class TestLog : CliktCommand() {
  class Bar {
    val log = Log.log {} // TODO: lazy?
    fun f() {
      println(this.javaClass.name)
      log.error("error")
      log.warn("warn")
      log.info("info")
      log.debug("debug")
      log.trace("trace")
    }
  }
  override fun run() {
    Bar().f()
    echo("executing")
  }
}

class BuildInfo : CliktCommand(help = "Display information about how `jade` was built") {
  // TODO: --long --short
  override fun run() {
    with(BuildInformation) {
      println(
        """$versionMessage
          |Build tools: Kotlin $kotlinVersion, Gradle $gradleVersion, Java $javaVersion
          |Build time: $buildTime
          |Dependencies:
        """.trimMargin()
      )
    }
    for (d in BuildInformation.dependencies) {
      println("  ${d.first} (configuration: ${d.second})")
    }
    println("Compile-time system properties:")
    for (l in BuildInformation.systemProperties) {
      println("  ${l.first}=${l.second}")
    }
    println("Runtime system properties:")
    for (
      p in System
        .getProperties()
        .toList()
        .sortedBy { it.first.toString() }
        .filter { it.first.toString().matches("(java|os)\\..*".toRegex()) }
    ) {
      println("  ${p.first}=${p.second}")
    }
  }
}

class Decompile : CliktCommand(help = "Display information about how `jade` was built") {
  // TODO: --include-file --exclude-file --include-class --exclude-class --include-cxt-file --include-cxt-class
  // --filter=+dir=

  // TODO: convert from File to Path (Path is more modern)
  val files: List<File> by argument(
    name = "PATH",
    help = "Files or directories to decompile",
  ).file(mustExist = true).multiple(required = true)
  val op by option().int().required()

  override fun run() {
    // TODO("implemenet decompile")
    val vfs = Vfs()
    for (file in files) {
      vfs.dir(file)
    }
    for ((k, _) in vfs.result) {
      println("k $k")
    }
  }
}

class Compile : CliktCommand(help = "Compile a java file") {
  override fun run() {
    // TODO: Use a JavaAgent of a nested compiler to test whether the code compiles
    // TODO: Test whether it compiles under different Java versions
    // TODO: Back-off if compilation fails
    TODO("implement compile")
  }
}

// TODO: commands for decompiling with other decompilers

class Diff : CliktCommand(help = "Compare class files") {
  override fun run() {
    TODO("implement diff")
  }
}

// TODO: rename to loggers?
class Logs : CliktCommand(help = "Lists available logs") {
  override fun run() {
    // Log.listLogs()
    TODO("implement list-logs")
  }
}
