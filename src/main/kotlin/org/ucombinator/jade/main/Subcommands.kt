@file:Suppress(
  "MISSING_KDOC_CLASS_ELEMENTS",
  "MISSING_KDOC_TOP_LEVEL",
  "UndocumentedPublicClass",
  "UndocumentedPublicProperty",
)

// Since subcommand and their options are documented by the help message in the commands themselves, this file is
// separate from Main.kt so we can suppress lint warnings about undocumented classes and properties.
package org.ucombinator.jade.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import org.ucombinator.jade.util.Log
import java.io.File

class BuildInfo : CliktCommand(help = "Display information about how `jade` was built") {
  // TODO: --long --short
  override fun run() {
    with(BuildInformation) {
      echo(
        """
          $versionMessage
          Build tools: Kotlin $kotlinVersion, Gradle $gradleVersion, Java $javaVersion
          Build time: $buildTime
          Dependencies:
        """.trimIndent()
      )
    }
    for (d in BuildInformation.dependencies) {
      echo("  ${d.first} (configuration: ${d.second})")
    }
    echo("Compile-time system properties:")
    for (l in BuildInformation.systemProperties) {
      echo("  ${l.first}=${l.second}")
    }
    echo("Runtime system properties:")
    val properties = System.getProperties().toList()
      .sortedBy { it.first.toString() }
      .filter { it.first.toString().matches("(java|os)\\..*".toRegex()) }
    for (p in properties) {
      echo("  ${p.first}=${p.second}")
    }
  }
}

class Decompile : CliktCommand(help = "Decompile a class file") {
  // TODO: --include-file --exclude-file --include-class --exclude-class --include-cxt-file --include-cxt-class
  // --filter=+dir=
  // TODO: --classpath

  // TODO: File vs Path vs other

  val files: List<File> by argument(
    name = "PATH",
    help = "Files or directories to decompile",
  ).file(mustExist = true).multiple(required = true)

  val outputDir: File by argument(
    name = "PATH",
    help = "Directory to write to",
  ).file(mustExist = true)

  override fun run() {
    org.ucombinator.jade.decompile.Decompile.main(files, outputDir)
  }
}

class Compile : CliktCommand(help = "Compile a java file") {
  // TODO: factor common parameters with Decompile
  val files: List<File> by argument(
    name = "PATH",
    help = "Files or directories to compile",
  ).file(mustExist = true).multiple(required = true)

  override fun run() {
    // TODO: Use a JavaAgent of a nested compiler to test whether the code compiles
    // TODO: Test whether it compiles under different Java versions
    // TODO: Back-off if compilation fails
    org.ucombinator.jade.compile.Compile.main(files)
  }
}

// TODO: commands for decompiling with other decompilers

class Diff : CliktCommand(help = "Compare class files") {
  val old: File by argument(
    name = "PATH",
    help = "TODO",
  ).file(mustExist = true)

  val new: File by argument(
    name = "PATH",
    help = "TODO",
  ).file(mustExist = true)

  override fun run() {
    org.ucombinator.jade.diff.Diff.main(old, new)
  }
}

class Loggers : CliktCommand(help = "Lists available loggers") {
  val test: Boolean by option(help = "TODO").flag(default = false)

  override fun run() {
    // TODO: shows only initialized loggers
    for (log in Log.loggers()) {
      echo(log.name)
      if (test) {
        log.error("error in ${log}")
        log.warn("warn in ${log}")
        log.info("info in ${log}")
        log.debug("debug in ${log}")
        log.trace("trace in ${log}")
        echo()
      }
    }
  }
}

class DownloadIndex : CliktCommand(help = "Download index of all files") {
  val indexFile: File by argument(name = "INDEX").file(canBeDir = false)
  val authFile: File? by option(metavar = "FILE").file(canBeDir = false, mustBeReadable = true)
  val resume: Boolean by option().flag(default = false)
  val maxResults: Long by option().long().default(0)
  val pageSize: Long by option().long().default(0)
  val prefix: String? by option()
  val startOffset: String? by option()
  val flushFrequency: Long by option().long().default(1L shl 14)

  override fun run() {
    org.ucombinator.jade.maven.DownloadIndex.main(
      indexFile,
      authFile,
      resume,
      maxResults,
      pageSize,
      prefix,
      startOffset,
      flushFrequency
    )
  }
}

class DownloadMaven : CliktCommand(help = "Download Maven data") {
  val index: File by argument().file(mustExist = true, mustBeReadable = true, canBeDir = false)
  val localRepo: File by argument().file(mustExist = true, canBeFile = false)
  val jarLists: File by argument().file(mustExist = true, canBeFile = false)
  val reverse: Boolean by option().flag(default = false)
  val shuffle: Boolean by option().flag(default = false)

  override fun run() {
    org.ucombinator.jade.maven.DownloadMaven(index, localRepo, jarLists, reverse, shuffle).run()
  }
}
