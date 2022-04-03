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
    Loggers(),
    DownloadIndex(),
    DownloadMetadata(),
    DownloadPoms(),
    DownloadParents(),
    DownloadDependencies(),
    MavenAuto(),
    DownloadPoms2(),
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
        1 -> LogSetting("", Level.toLevel(r[0]))
        2 -> LogSetting(r[0], Level.toLevel(r[1]))
        else -> TODO("impossible")
      }
    }
  }.default(listOf())
//     showDefaultValue = CommandLine.Help.Visibility.NEVER,

  val logCallerDepth: Int by option(
    metavar = "DEPTH",
    help = "Number of callers to print after log messages",
  ).int().default(0)

  val ioThreads: Int? by option().int()

  val wait: Boolean by option(
    help = "Wait for input from user before running.  This allows time for a debugger to attach to this process."
  ).flag(
    "--no-wait",
    default = false
  )

  override fun run() {
    if (ioThreads !== null) System.setProperty(kotlinx.coroutines.IO_PARALLELISM_PROPERTY_NAME, ioThreads.toString())

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
    val log = Log {} // TODO: lazy?
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

  val files: List<File> by argument(
    name = "PATH",
    help = "Files or directories to decompile",
  ).file(mustExist = true).multiple(required = true)

  override fun run() {
    org.ucombinator.jade.decompile.Decompile.main(files)
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

class Loggers : CliktCommand(help = "Lists available loggers") {
  override fun run() {
    Log.listLoggers()
  }
}

class DownloadIndex : CliktCommand(help = "Lists available loggers") {
  val indexFile: File by argument(
    name = "INDEX"
  ).file(canBeDir = false)

  val authFile: File? by option(
    metavar = "FILE"
  ).file(canBeDir = false, mustBeReadable = true)

  val resume: Boolean by option().flag(default = false)
  val maxResults: Long by option().long().default(0)
  val pageSize: Long by option().long().default(0)
  val prefix: String? by option()
  val startOffset: String? by option()
  val flushFrequency: Long by option().long().default(0)

  // val threads: Int by option().int().default(0)

  // resume: Boolean = false,
  // maxResults: Long = 0L,
  // pageSize: Long = 0L,
  // prefix: String? = null,
  // startOffset: String? = null

  override fun run() {
    org.ucombinator.jade.maven.DownloadIndex.main(indexFile, authFile, resume, maxResults, pageSize, prefix, startOffset, flushFrequency)
  }
}

class DownloadMetadata : CliktCommand(help = "Lists available loggers") {
  val indexFile: File by argument(
    name = "INDEX"
  ).file(canBeDir = false, mustBeReadable = true)

  val destDir: File by argument(
    name = "DEST"
  ).file(mustExist = true, canBeFile = false)

  val authFile: File? by option(
    metavar = "FILE"
  ).file(canBeDir = false, mustBeReadable = true)

  override fun run() {
    org.ucombinator.jade.maven.DownloadMetadata.main(indexFile, destDir, authFile)
  }
}

class DownloadPoms : CliktCommand(help = "Lists available loggers") {
  val srcDir: File by argument()
    .file(mustExist = true, canBeFile = false)

  val dstDir: File by argument()
    .file(mustExist = true, canBeFile = false)

  val authFile: File? by option(
    metavar = "FILE"
  ).file(canBeDir = false, mustBeReadable = true)

  override fun run() {
    org.ucombinator.jade.maven.DownloadPoms.main(srcDir, dstDir, authFile)
  }
}

class DownloadParents : CliktCommand(help = "Lists available loggers") {
  val srcDir: File by argument()
    .file(mustExist = true, canBeFile = false)

  val dstDir: File by argument()
    .file(mustExist = true, canBeFile = false)

  val authFile: File? by option(
    metavar = "FILE"
  ).file(canBeDir = false, mustBeReadable = true)

  override fun run() {
    org.ucombinator.jade.maven.DownloadParents.main(srcDir, dstDir, authFile)
  }
}

class DownloadDependencies : CliktCommand(help = "Lists available loggers") {
  val srcDir: File by argument()
    .file(mustExist = true, canBeFile = false)

  val dstDir: File by argument()
    .file(mustExist = true, canBeFile = false)

  val authFile: File? by option(
    metavar = "FILE"
  ).file(canBeDir = false, mustBeReadable = true)

  override fun run() {
    org.ucombinator.jade.maven.DownloadDependencies.main(srcDir, dstDir, authFile)
  }
}

class MavenAuto : CliktCommand(help = "Lists available loggers") {
  override fun run() {
    org.ucombinator.jade.maven.MavenAuto.main()
  }
}

class DownloadPoms2 : CliktCommand(help = "Lists available loggers") {
  val index: File by argument()
    .file(mustExist = true, mustBeReadable = true, canBeDir = false)

  val localRepo: File by argument()
    .file(mustExist = true, canBeFile = false)

  override fun run() {
    org.ucombinator.jade.maven.DownloadPoms2.main(index, localRepo)
  }
}
