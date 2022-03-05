import org.ucombinator.jade.gradle.GenerateClassfileFlags
import org.ucombinator.jade.gradle.GitVersionPlugin
import java.text.SimpleDateFormat
import java.util.Date

repositories {
  mavenCentral()
}

plugins {
  kotlin("jvm") // version "1.5.31"
  application

  // For parsing signatures
  antlr

  // Documentation
  id("org.jetbrains.dokka") version "1.5.31" // Adds: ./gradlew dokka{Gfm,Html,Javadoc,Jekyll}

  // Code formatting
  id("com.diffplug.spotless") version "6.3.0" // Adds: ./gradlew spotlessCheck

  // Code coverage
  id("jacoco") // Adds: ./gradlew jacocoTestReport
  id("org.jetbrains.kotlinx.kover") version "0.5.0" // Adds: ./gradlew koverMergedHtmlReport

  // Licenses
  id("com.github.jk1.dependency-license-report") version "2.1" // Adds: ./gradlew generateLicenseReport

  // Dependency versions
  id("com.github.ben-manes.versions") version "0.42.0" // Adds: ./gradlew dependencyUpdates
}

dependencies {
  // NOTE: these are sorted alphabetically

  // For parsing signatures
  antlr("org.antlr:antlr4:4.9.3")

  // Testing
  testImplementation(kotlin("test"))

  // Logging (see also io.github.microutils:kotlin-logging-jvm)
  implementation("ch.qos.logback:logback-classic:1.2.6")

  // Command-line argument parsing
  implementation("com.github.ajalt.clikt:clikt:3.4.0")

  // Abstract Syntax Trees for the Java language
  implementation("com.github.javaparser:javaparser-core:3.24.0") // Main library
  implementation("com.github.javaparser:javaparser-core-serialization:3.24.0") // Serialization to/from JSON
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.0") // Resolving symbols and identifiers
  // Omitting the JavaParser "parent" package as it is just metadata
  // Omitting the JavaParser "generator" and "metamodel" packages as they are just for building JavaParser

  // Logging (see also ch.qos.logback:logback-classic)
  implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")

  // Compressed files
  implementation("org.apache.commons:commons-compress:1.21")

  // Vertex and edge graphs
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("org.jgrapht:jgrapht-ext:1.5.1")
  // implementation("org.jgrapht:jgrapht-guava:1.5.1")
  implementation("org.jgrapht:jgrapht-io:1.5.1")
  implementation("org.jgrapht:jgrapht-opt:1.5.1")

  // `.class` file parsing and analysis
  implementation("org.ow2.asm:asm:9.2")
  implementation("org.ow2.asm:asm-analysis:9.2")
  implementation("org.ow2.asm:asm-commons:9.2")
  // implementation("org.ow2.asm:asm-test:9.2")
  implementation("org.ow2.asm:asm-tree:9.2")
  implementation("org.ow2.asm:asm-util:9.2")
}

application {
  mainClass.set("org.ucombinator.jade.main.MainKt")
}

// version = "0.1.0" // Uncomment to manually set the version
apply<GitVersionPlugin>()

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    // ktfmt()
    ktlint().userData(
      mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2",
        // "disabled_rules" to "no-wildcard-imports",
        // "verbose" to "true",
        // TODO: "space_before_extend_colon" to "false",
        // TODO: Unit return values
      )
    )
    // diktat()
    // prettier()
  }
  kotlinGradle {
    // ktfmt()
    ktlint().userData(
      mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2",
      )
    )
    // diktat()
    // prettier()
  }
}

val antlrCharVocab by tasks.registering {
  doLast {
    val antlr = tasks.generateGrammarSource
    val dir = antlr.get().outputDirectory
    dir.mkdirs()
    val file = File(dir, "Character.tokens")

    file.bufferedWriter().use { w ->
      w.write("'\\0'=${0x0000}\n")
      w.write("'\\b'=${0x0008}\n")
      w.write("'\\t'=${0x0009}\n")
      w.write("'\\n'=${0x000a}\n")
      w.write("'\\f'=${0x000c}\n")
      w.write("'\\r'=${0x000d}\n")
      w.write("'\\\"'=${0x0022}\n")
      w.write("'\\''=${0x0027}\n")
      w.write("'\\\\'=${0x005c}\n")
      for (i in 0..127) {
        w.write("CHAR_x${"%02X".format(i)}=$i\n")
        w.write("CHAR_u${"%04X".format(i)}=$i\n")
        if (i >= 32 && i <= 126 && i.toChar() != '\'' && i.toChar() != '\\') {
          w.write("'${i.toChar()}'=$i\n")
        }
      }
    }
  }
}
tasks.generateGrammarSource {
  dependsOn(antlrCharVocab)
  arguments.add("-no-listener")
}

fun generateSrc(fileName: String, code: String) {
  // TODO: use spotless for formatting
  // val scalafmt = org.scalafmt.interfaces.Scalafmt
  //   .create(this.getClass.getClassLoader)
  //   .withReporter(new ScalafmtSbtReporter(streamsValue.log, new java.io.OutputStreamWriter(streamsValue.binary()), true));
  // if (flagsCode != scalafmt.format(scalafmtConfig.value.toPath(), sourceFile.toPath(), flagsCode)) {
  //   streamsValue.log.warn(f"\nGenerated file isn't formatted properly: ${sourceFile}\n\n")
  // }
  val generatedSrcDir = File(buildDir, "generated/sources/jade/src/main/kotlin")
  generatedSrcDir.mkdirs()
  kotlin.sourceSets["main"].kotlin.srcDir(generatedSrcDir)
  val file = File(generatedSrcDir, fileName)
  file.writeText(code)
}

val generateClassfileFlags by tasks.registering {
  doLast {
    // TODO: avoid running when unchanged
    val code = GenerateClassfileFlags.code(
      File(projectDir, "src/main/kotlin/org/ucombinator/jade/classfile/Flags.txt")
        .readText(Charsets.UTF_8)
    )
    generateSrc("Flags.kt", code)
  }
}

val generateBuildInfo by tasks.registering {
  doLast {
    // TODO: avoid running when unchanged

    // project.kotlin.target.platformType
    // project.kotlin.target.targetName
    // project.kotlin.target.attributes
    // project.kotlin.target.name
    // project.sourceSets.main.get().runtimeClasspath

    val systemProperties = System
      .getProperties()
      .toList()
      .filter { it.first.toString().matches("(java|os)\\..*".toRegex()) }
      .map { "    \"${it.first}\" to \"${it.second}\"," }
      .sorted()
      .joinToString("\n")

    val dependencies = project
      .configurations
      .flatMap { it.dependencies }
      .filterIsInstance<ExternalDependency>()
      .map { "    \"${it.group}:${it.name}:${it.version}\" to \"${it.targetConfiguration ?: "default"}\"," }
      .sorted()
      .joinToString("\n")

    fun field(fieldName: String, value: Any?): String {
      val v = if (value === null) { "null" } else { "\"$value\"" }
      return (
        """  /** The value is $v. */
          |  val ${fieldName.trim()}: String? = $v
          |""".trimMargin()
        )
    }

    val code = """// Do not edit this file by hand.  It is generated by `gradle`.
      |
      |package org.ucombinator.jade.main
      |
      |/** This object was generated by `gradle`. */
      |object BuildInformation {
      |${field("group        ", project.group)}
      |${field("name         ", project.name)}
      |${field("version      ", project.version)}
      |${field("description  ", project.description)}
      |${field("kotlinVersion", project.kotlin.coreLibrariesVersion)}
      |${field("javaVersion  ", System.getProperty("java.version"))}
      |${field("gradleVersion", project.gradle.gradleVersion)}
      |${field("buildTime    ", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(Date()))}
      |${field("status       ", project.status)}
      |${field("path         ", project.path)}
      |  // TODO: javadoc
      |  val dependencies: List<Pair<String, String>> = listOf(
      |$dependencies
      |  )
      |
      |  // TODO: javadoc
      |  val systemProperties: List<Pair<String, String>> = listOf(
      |$systemProperties
      |  )
      |
      |  val versionMessage = "${'$'}name version ${'$'}version (https://github.org/ucombinator/jade)"
      |}
      |""".trimMargin()

    generateSrc("BuildInformation.kt", code)
  }
}

tasks.withType<Test> {
  // Use JUnit Platform for unit tests.
  useJUnitPlatform()

  this.testLogging {
    this.showStandardStreams = true
  }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
  dokkaSourceSets {
    named("main") {
      includes.from("Module.md")
    }
  }
}

// For why we have to fully qualify KotlinCompile see:
// https://stackoverflow.com/questions/55456176/unresolved-reference-compilekotlin-in-build-gradle-kts
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  // Avoid the warning: 'compileJava' task (current target is 11) and
  // 'compileKotlin' task (current target is 1.8) jvm target compatibility should
  // be set to the same Java version.
  kotlinOptions { jvmTarget = project.java.targetCompatibility.toString() }

  dependsOn(tasks.generateGrammarSource)
  dependsOn(tasks.generateTestGrammarSource)
  dependsOn(generateClassfileFlags)
  dependsOn(generateBuildInfo)
}
