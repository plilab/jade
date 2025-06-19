@file:Suppress(
  "MISSING_KDOC_CLASS_ELEMENTS",
  "MISSING_KDOC_TOP_LEVEL",
  "UndocumentedPublicClass",
  "UndocumentedPublicProperty",
)

// Since subcommand and their options are documented by the help message in the commands themselves, this file is
// separate from Main.kt so we can suppress lint warnings about undocumented classes and properties.
package org.ucombinator.jade.main

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.types.file
import org.ucombinator.jade.util.Log

import java.io.File
import java.net.URI
import kotlin.time.Duration

class Decompile : JadeCommand() {
  override fun help(context: Context) = "Decompile a class file"

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

class Compile : JadeCommand() {
  override fun help(context: Context) = "Compile a java file"

  // TODO: factor common parameters with Decompile
  val options: List<String> by option().multiple()
  val classes: List<String> by option().multiple()

  val files: List<File> by argument(
    name = "PATH",
    help = "Files or directories to compile",
  ).file(mustExist = true).multiple(required = true)

  override fun run() {
    org.ucombinator.jade.compile.Compile.main(null, null, null, options, classes, files)
    // TODO: instead of null use: System.out.writer() and others
  }
}

// TODO: commands for decompiling with other decompilers

class Diff : JadeCommand() {
  override fun help(context: Context) = "Compare class files"

  val old: File by argument(
    name = "PATH",
    help = "First .class file",
  ).file(mustExist = true)

  val new: File by argument(
    name = "PATH",
    help = "Second .class file",
  ).file(mustExist = true)

  override fun run() {
    org.ucombinator.jade.diff.Diff.main(old, new)
  }
}

class Maven : NoOpJadeCommand() {
  override fun help(context: Context) = """
      Commands for operating with Maven.

      Common values for `remote` include:

      - https://repo.maven.apache.org/maven2/
      - https://repo1.maven.org/maven2/
      - https://maven-central.storage-download.googleapis.com/maven2/
      - https://maven-central-eu.storage-download.googleapis.com/maven2/
      - https://maven-central-asia.storage-download.googleapis.com/maven2/
    """.trimIndent()

  // TODO: options such as proxy to mirror that match `mvn` options: https://maven.apache.org/settings.html
  // TODO: use `::URI`

  class Mirrors : JadeCommand() {
    override fun help(context: Context) = """Print the mirrors of a maven repository"""

    val remote: URI by option().convert{ URI(it) }.default(URI(org.ucombinator.jade.maven.Maven.mavenCentral.first.url))

    override fun run() {
      org.ucombinator.jade.maven.Mirrors.main(remote)
    }
  }
  // TODO: Clikt: metavar based on type (for URI)

  class Index : JadeCommand() {
    override fun help(context: Context) = "Download the index from a remote Maven repository"

    val remote: URI by option(metavar = "URI", help = "URI of the repository to download from").convert{ URI(it) }.default(URI(org.ucombinator.jade.maven.Maven.mavenCentral.first.url))

    val local: File by argument(help = "Path to the local directory in which to store the index").file(mustExist = true, canBeFile = false, mustBeWritable = true)

    override fun run() {
      org.ucombinator.jade.maven.Index.main(remote, local)
    }
  }

  class IndexToJson : JadeCommand() {
    override fun help(context: Context) = "Print a Maven index to stdout as JSON lines"

    val index: Boolean by option(help = "Whether to print `INDEX` records").flag("--no-index", default = true)
    val chunk: Boolean by option(help = "Whether to print `CHUNK` records").flag("--no-chunk", default = true)
    val record: Boolean by option(help = "Whether to print `RECORD` records").flag("--no-record", default = true)
    val expandedRecord: Boolean by option(help = "Whether to print `EXPANDED_RECORD` records").flag("--no-expanded-record", default = true)

    val indexFile: File by argument().file(mustExist = true, canBeDir = false, mustBeReadable = true)

    override fun run() {
      org.ucombinator.jade.maven.IndexToJson.main(indexFile, index, chunk, record, expandedRecord)
    }
  }

  class Versions : JadeCommand() {
    override fun help(context: Context) = "Select artifact versions"

    // TODO: factor into ParallelCommand (or a mixin?)
    // TODO: add io-threads
    val shuffle: Boolean by option(help = "Whether to randomize the order of the inputs").flag("--no-shuffle", default = false)
    val timeout: Duration by option(help = "How long to let an input run before timing it out").convert { Duration.parse(it) }.default(Duration.parse("5m")) // TODO: how to do infinity?

    val localRepoDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)
    val versionsDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false) // TODO: as option and use stdout if not set
    val artifacts: List<Pair<String, String>> by
      argument().convert { org.ucombinator.jade.maven.Maven.coordinate(it) }.multiple(required = true)
      // TODO: argument().shuffled(shuffle)

    override fun run() {
      val shuffledArtifacts = if (shuffle) artifacts.shuffled() else artifacts // TODO: factor
      org.ucombinator.jade.maven.Versions.main(timeout, localRepoDir, versionsDir, shuffledArtifacts)
    }
  }

  class Dependencies : JadeCommand() {
    override fun help(context: Context) = "TODO"

    val shuffle: Boolean by option(help = "Whether to randomize the order of the inputs").flag("--no-shuffle", default = false)
    val timeout: Duration by option(help = "How long to let an input run before timing it out").convert { Duration.parse(it) }.default(Duration.parse("5m")) // TODO: how to do infinity?

    // TODO: class Dependencies { remoterepos(default=central) localIndex artifacts() -> stdout or outputDir (local repo) }
    val localRepoDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)
    val dependenciesDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false) // TODO: as option and use stdout if not set
    val artifacts: List<org.eclipse.aether.artifact.Artifact> by
      argument().convert { org.eclipse.aether.artifact.DefaultArtifact(it)}.multiple(required = true)

    override fun run() {
      val shuffledArtifacts = if (shuffle) artifacts.shuffled() else artifacts
      org.ucombinator.jade.maven.Dependencies.main(timeout, localRepoDir, dependenciesDir, shuffledArtifacts)
    }
  }

  // TODO: class DependenciesToFileNames

  // TODO: class ArtifactUrl

  class ClearLocks : JadeCommand() {
    override fun help(context: Context) = "TODO"

    override fun run() {
      // TODO: command to clear locks
      // ../repo/local-repo/org/bgee/log4jdbc-log4j2/log4jdbc-log4j2-jdbc4.1/maven-metadata-google-maven-central-ap.xml.6707912453454501025.tmp
      // $ find ~/a/local/jade2/maven '(' -name \*.part -o -name \*.lock -o -size 0 ')' -type f -print0 | xargs -0 rm -v
      // $ find ~/a/local/jade2/jar-lists/ -size 0 -type f -print0 | xargs -0 rm -v
      TODO()
    }
  }

  class Download : JadeCommand() {
    override fun help(context: Context) = "TODO"

    val shuffle: Boolean by option().flag("--no-shuffle", default = false)
    val timeout: Duration by option().convert { Duration.parse(it) }.default(Duration.parse("5m")) // TODO: how to do infinity?

    val localRepoDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false)
    val artifactsDir: File by argument().file() // TODO: (mustExist = true, mustBeReadable = true, canBeDir = false) // TODO: as option and use stdout if not set
    val artifacts: List<org.eclipse.aether.artifact.Artifact> by
      argument().convert { org.eclipse.aether.artifact.DefaultArtifact(it)}.multiple(required = true)

    override fun run() {
      val shuffledArtifacts = if (shuffle) artifacts.shuffled() else artifacts
      org.ucombinator.jade.maven.Download.main(timeout, localRepoDir, artifactsDir, shuffledArtifacts)
    }
  }
}

class About : NoOpJadeCommand() {
  override fun help(context: Context) = "Commands about `jade`"

  class BuildInfo : JadeCommand() {
    override fun help(context: Context) = "Show information about how `jade` was built"

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

  class Loggers : JadeCommand() {
    override fun help(context: Context) = "List available loggers"

    val test: Boolean by option(help = "Send test messages to all loggers").flag(default = false)

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
