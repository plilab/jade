@file:Suppress(
  "MISSING_KDOC_CLASS_ELEMENTS",
  "MISSING_KDOC_TOP_LEVEL",
  "UndocumentedPublicClass",
  "UndocumentedPublicProperty",
)

// Since subcommand and their options are documented by the help message in the commands themselves, this file is
// separate from Main.kt so we can suppress lint warnings about undocumented classes and properties.
package org.ucombinator.jade.main

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import org.ucombinator.jade.util.Log
import java.io.File
import java.net.URI

class Decompile : JadeCommand(help = "Decompile a class file") {
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

class Compile : JadeCommand(help = "Compile a java file") {
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

class Diff : JadeCommand(help = "Compare class files") {
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

class Maven : NoOpJadeCommand(help = "TODO") {
  // TODO: options such as proxy to mirror that match `mvn` options: https://maven.apache.org/settings.html

  class Mirrors : JadeCommand(help = "TODO") {
    // https://repo1.maven.org/maven2/.meta/repository-metadata.xml
    override fun run() {
      TODO()
    }
  }

  class Index : JadeCommand(help = "TODO") {
    // TODO: use `::URI`
    // https://repo1.maven.org/maven2/.index/
    // https://maven-central-asia.storage-download.googleapis.com/maven2/.index/ (or mirror file)

    val remote: URI by argument().convert{ URI(it) } // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)
    val local: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)

    override fun run() {
      org.ucombinator.jade.maven.Index.main(remote, local)
    }
  }

  // TODO: options first then arguments
  class IndexToJson : JadeCommand(help = "TODO") {
    val index: Boolean by option().flag("--no-index", default = true)
    val chunk: Boolean by option().flag("--no-chunk", default = true)
    val record: Boolean by option().flag("--no-record", default = true)
    val expandedRecord: Boolean by option().flag("--no-expanded-record", default = true)

    val remote: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)

    override fun run() {
      org.ucombinator.jade.maven.IndexToJson.main(remote, index, chunk, record, expandedRecord)
    }
  }

  class Versions : JadeCommand(help = "TODO") {
    val shuffle: Boolean by option().flag("--no-shuffle", default = false)

    val localRepoDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)
    val versionsDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false) // TODO: as option and use stdout if not set
    val coordinates: List<Pair<String, String>> by
      argument().convert { org.ucombinator.jade.maven.Versions.coordinate(it) }.multiple(required = true) // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)

    override fun run() {
      val shuffledCoordinates = if (shuffle) coordinates.shuffled() else coordinates
      org.ucombinator.jade.maven.Versions.main(localRepoDir, versionsDir, shuffledCoordinates)
    }
  }

  class Dependencies : JadeCommand(help = "TODO") {
    val shuffle: Boolean by option().flag("--no-shuffle", default = false)

    // TODO: class Dependencies { remoterepos(default=central) localIndex artifacts() -> stdout or outputDir (local repo) }
    val localRepoDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)
    val dependenciesDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false) // TODO: as option and use stdout if not set
    val coordinates: List<org.eclipse.aether.artifact.Artifact> by
      argument().convert { org.eclipse.aether.artifact.DefaultArtifact(it)}.multiple(required = true) // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)

    override fun run() {
      val shuffledCoordinates = if (shuffle) coordinates.shuffled() else coordinates
      org.ucombinator.jade.maven.Dependencies.main(localRepoDir, dependenciesDir, shuffledCoordinates)
    }
  }
  // TODO: class DependenciesToFileNames

  // TODO: class Download
}

class Meta : NoOpJadeCommand(help = "TODO") { // TODO: rename (to "about"?)
  class BuildInfo : JadeCommand(help = "Show information about how `jade` was built") {
    // TODO: --long --short
    override fun run() {
      with(BuildInformation) {
        echo(
          """
            $versionMessage
            Build tools: Kotlin $kotlinVersion, Gradle $gradleVersion, Java $javaVersion
            Build time: $buildTime
            Dependencies:
          """.trimIndent(),
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

  class Loggers : JadeCommand(help = "List available loggers") {
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
}
