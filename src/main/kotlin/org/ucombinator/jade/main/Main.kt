package org.ucombinator.jade.main

import mu.KotlinLogging
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.DynamicCallerConverter
import ch.qos.logback.classic.Level

// TODO: analysis to ensure using only the canonical constructor (helps with detecting forward version changes) (as a compiler plugin?)
// TODO: exit code list
// TODO: exit codes

////////////////
// Top-level command

// TODO: description
// TODO: header/footer?
// TODO: aliases, description, defaultValueProvider
// TODO: have build generate documentation

// TODO: java -cp lib/jade/jade.jar picocli.AutoComplete -n jade org.ucombinator.jade.main.Main (see https://picocli.info/autocomplete.html)


fun main(args: Array<String>): Unit =
  Jade().subcommands(
    // classOf[HelpCommand],
    BuildInfo(),
    Decompile(),
    Compile(),
    Diff(),
    Logs(),
    // classOf[ManPageGenerator],
    // classOf[GenerateCompletion],
  ).main(args)
// TODO: harmonize sub-command names for `gen-manpage` and `generate-completion`

//   val commandLine: CommandLine = new CommandLine(new Main())
//   commandLine.setAbbreviatedOptionsAllowed(true)
//   commandLine.setAbbreviatedSubcommandsAllowed(true)
//   commandLine.setOverwrittenOptionsAllowed(true)
//   def main(args: Array[String]): Unit = {
//     System.exit(commandLine.execute(args: _*))
//   }
//   def versionString: String = {
//     import org.ucombinator.jade.main.BuildInfo._
//     f"${name} version ${version} (https://github.org/ucombinator/jade)"
//   }

//   mixinStandardHelpOptions = true,
//   requiredOptionMarker = '*', // TODO: put in documentation string
//   showAtFileInUsageHelp = true,
//   showDefaultValues = true,
//   showEndOfOptionsDelimiterInUsageHelp = true,
//   versionProvider = classOf[VersionProvider],
class Jade: CliktCommand() {
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

  val count: Int by option(help="Number of greetings").int().default(1)

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
      println("Waiting for user.  Press \"Enter\" to continue.")
      readLine()
    }
  }
}

// class VersionProvider extends CommandLine.IVersionProvider {
//   override def getVersion: Array[String] = { Array[String](Main.versionString) }
// }

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

class BuildInfo: CliktCommand(help="Display information about how `jade` was built") {
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
    echo("xexecuting")

    // import org.ucombinator.jade.main.BuildInfo._
    // println(f"""${Main.versionString}
    //            |Build tools: Scala ${scalaVersion}, SBT ${sbtVersion}
    //            |Build time: ${builtAtString} ${builtAtMillis}ms
    //            |Libraries:""".stripMargin)
    // for (l <- libraryDependencies.sorted) {
    //   println("  " + l)
    // }

  }
}

class Decompile: CliktCommand(help="Display information about how `jade` was built") {
  // --include-file --exclude-file --include-class --exclude-class
  // --include-cxt-file --include-cxt-class

  // @Parameters(paramLabel = "<path>", arity = "1..*", description = Array("Files or directories to decompile"), `type` = Array(classOf[java.nio.file.Path]))
  // var path: java.util.List[java.nio.file.Path] = _

  override fun run() {
    echo("executing")
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

class Diff: CliktCommand(help="Compare class files") {
  override fun run() {
    TODO("implement diff")
  }
}

class Logs: CliktCommand(help="Lists available logs") {
  override fun run() {
    // Log.listLogs()
  }
}
