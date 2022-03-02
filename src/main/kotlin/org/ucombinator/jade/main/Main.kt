package org.ucombinator.jade.main

import mu.KotlinLogging
import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.* // ktlint-disable no-wildcard-imports
import com.github.ajalt.clikt.parameters.options.* // ktlint-disable no-wildcard-imports
import com.github.ajalt.clikt.parameters.types.* // ktlint-disable no-wildcard-imports
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.DynamicCallerConverter
import ch.qos.logback.classic.Level
import org.ucombinator.jade.main.BuildInformation
// TODO: analysis to ensure using only the canonical constructor (helps with detecting forward version changes) (as a compiler plugin?)
// TODO: exit code list
// TODO: exit codes

////////////////
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

//   requiredOptionMarker = '*', // TODO: put in documentation string
//   showAtFileInUsageHelp = true,
//   showDefaultValues = true,
//   showEndOfOptionsDelimiterInUsageHelp = true,
class Jade: CliktCommand() {
  init {
    versionOption(BuildInformation.version!!, message= { BuildInformation.versionMessage })
  }
//   @Option(
//     names = Array("--log"),
//     paramLabel = "LEVEL",
//     description = Array(
//       "Set the logging level where LEVEL is LVL or NAME=LVL.",
//       "",
//       "LVL is one of (case insensitive):",
//       "  off info warning error debug trace all",
//       "NAME is a qualified package or class name and is relative to `org.ucombinator.jade` unless prefixed with `.`."
//     ),
//     split = ",",
//     showDefaultValue = CommandLine.Help.Visibility.NEVER,
//     converter = Array(classOf[LevelConverter]),
//   )
//   // TODO: check --help
  val log = listOf<LogSetting>()

  val logCallerDepth: Int by
    option(
      metavar="DEPTH",
      help="Number of callers to print after log messages",
    ).int().default(0)

  val wait: Boolean by
    option(
      help="Wait for input from user before running (useful when attaching to the process)")
    .flag(
      "--no-wait",
      default=false)

  override fun run() {
    DynamicCallerConverter.depthEnd = logCallerDepth

    for ((name, lvl) in log) {
      // TODO: warn if log exists
      // TODO: warn if no such class or package (and suggest qualifications)
      val parsedName =
        if (name.startsWith(".")) { name.substring(1) }
        else if (name == "") { "" }
        else { Log.prefix + lvl }
      Log.getLog(parsedName).setLevel(lvl)
    }

    if (wait) {
      // TODO: from TTY not stdin/stdout
      // TODO: TermUi.prompt()
      println("Waiting for user.  Press \"Enter\" to continue.")
      readLine()
    }
  }
}

data class LogSetting(val name: String, val lvl: Level)
// class LevelConverter extends ITypeConverter[LogSetting] {
//   override def convert(value: String): LogSetting = {
//     val (name, level) = value.split("=") match {
//       case Array(l) => ("", Level.toLevel(l, null))
//       case Array(n, l) => (n, Level.toLevel(l, null))
//       case _ => throw new Exception("could not parse log level") // TODO: explain notation
//     }
//     if (level == null) throw new Exception(f"invalid level: ${level}") // TODO: "must be one of ..."
//     LogSetting(name, level)
//   }
// }

////////////////
// Sub-commands

class TestLog: CliktCommand() {
  class Bar {
    val logger = Log.logger {} // TODO: lazy?
    fun f() {
      println(this.javaClass.getName())
      logger.error("error")
      logger.warn("warn")
      logger.info("info")
      logger.debug("debug")
      logger.trace("trace")
    }
  }
  override fun run() {
    Bar().f()
    echo("executing")
  }
}

class BuildInfo: CliktCommand(help="Display information about how `jade` was built") {
  // TODO: --long --short
  override fun run() {
    with (BuildInformation) {
      println("""${versionMessage}
        |Build tools: Kotlin ${kotlinVersion}, Gradle ${gradleVersion}, Java ${javaVersion}
        |Build time: ${buildTime}
        |Dependencies:""".trimMargin())
    }
    for (d in BuildInformation.dependencies) {
      println("  ${d.first} (configuration: ${d.second})")
    }
    println("Compile-time system properties:")
    for (l in BuildInformation.systemProperties) {
      println("  ${l.first}=${l.second}")
    }
    println("Runtime system properties:")
    for (p in System
        .getProperties()
        .toList()
        .sortedBy { it.first.toString() }
        .filter { it.first.toString().matches("(java|os)\\..*".toRegex()) }) {
      println("  ${p.first}=${p.second}")
    }
  }
}

class Decompile: CliktCommand(help="Display information about how `jade` was built") {
  // TODO: --include-file --exclude-file --include-class --exclude-class --include-cxt-file --include-cxt-class

  // @Parameters(paramLabel = "<path>", arity = "1..*", description = Array("Files or directories to decompile"), `type` = Array(classOf[java.nio.file.Path]))
  // var path: java.util.List[java.nio.file.Path] = _

  override fun run() {
    TODO("implemenet decompile")
  }
}

class Compile: CliktCommand(help="Compile a java file") {
  override fun run() {
    // TODO: Use a JavaAgent of a nested compiler to test whether the code compiles
    // TODO: Test whether it compiles under different Java versions
    // TODO: Back-off if compilation fails
    TODO("implement compile")
  }
}

// TODO: commands for decompiling with other decompilers

class Diff: CliktCommand(help="Compare class files") {
  override fun run() {
    TODO("implement diff")
  }
}

// TODO: rename to loggers?
class Logs: CliktCommand(help="Lists available logs") {
  override fun run() {
    // Log.listLogs()
    TODO("implement list-logs")
  }
}
