import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.ucombinator.jade.gradle.GenerateClassfileFlags
import org.ucombinator.jade.gradle.GitVersionPlugin

import java.text.SimpleDateFormat
import java.util.Date

// TODO: fix warning: :jar: No valid plugin descriptors were found in META-INF/gradle-plugins

repositories {
  mavenCentral()
}

// TODO: explain: $ ./gradlew tasks
plugins {
  kotlin("jvm") // version matches buildSrc/build.gradle.kts
  application

  // Documentation
  id("org.jetbrains.dokka") version "1.9.20" // Adds: ./gradlew dokka{Gfm,Html,Javadoc,Jekyll}

  // Code Formatting
  id("com.saveourtool.diktat") version "2.0.0" // Adds: ./gradlew diktatCheck
  id("io.gitlab.arturbosch.detekt") version "1.23.7" // Adds: ./gradlew detekt
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1" // Adds: ./gradlew ktlintCheck

  // Code Coverage
  id("jacoco") // version built into Gradle // Adds: ./gradlew jacocoTestReport
  id("org.jetbrains.kotlinx.kover") version "0.8.3" // Adds: ./gradlew koverMergedHtmlReport

  // Dependency Versions and Licenses
  id("com.github.ben-manes.versions") version "0.51.0" // Adds: ./gradlew dependencyUpdates
  id("com.github.jk1.dependency-license-report") version "2.9" // Adds: ./gradlew generateLicenseReport
}

dependencies {
  // Testing
  testImplementation(kotlin("test"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")

  // Code Formatting
  // detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7") // We use org.jlleitschuh.gradle.ktlint instead to use the newest ktlint
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.7")
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.7")

  // NOTE: these are sorted alphabetically

  // Logging (see also io.github.oshai:kotlin-logging-jvm)
  implementation("ch.qos.logback:logback-classic:1.5.12")

  // Command-line argument parsing
  implementation("com.github.ajalt.clikt:clikt:5.0.1")
  implementation("com.github.ajalt.clikt:clikt-markdown:5.0.1")

  // Java source abstract syntax trees
  implementation("com.github.javaparser:javaparser-core:3.26.2") // Main library
  implementation("com.github.javaparser:javaparser-core-serialization:3.26.2") // Serialization to/from JSON
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.2") // Resolving symbols and identifiers
  // Omitting the JavaParser "parent" package as it is just metadata
  // Omitting the JavaParser "generator" and "metamodel" packages as they are just for building JavaParser

  // Logging (see also ch.qos.logback:logback-classic)
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

  // Compression
  implementation("org.apache.commons:commons-compress:1.27.1")
  implementation("com.github.luben:zstd-jni:1.5.6-7")

  // Parallelization
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

  // JSON
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

  // Graphs (vertex and edge)
  implementation("org.jgrapht:jgrapht-core:1.5.2")
  implementation("org.jgrapht:jgrapht-ext:1.5.2")
  // implementation("org.jgrapht:jgrapht-guava:1.5.2")
  implementation("org.jgrapht:jgrapht-io:1.5.2")
  implementation("org.jgrapht:jgrapht-opt:1.5.2")

  // Class files
  implementation("org.ow2.asm:asm:9.7.1")
  implementation("org.ow2.asm:asm-analysis:9.7.1")
  implementation("org.ow2.asm:asm-commons:9.7.1")
  // implementation("org.ow2.asm:asm-test:9.7.1")
  implementation("org.ow2.asm:asm-tree:9.7.1")
  implementation("org.ow2.asm:asm-util:9.7.1")

  // Maven
  // TODO: trim?
  implementation("org.apache.maven.indexer:indexer-core:7.1.5")
  implementation("org.apache.maven.indexer:indexer-reader:7.1.5")
  implementation("org.apache.maven.resolver:maven-resolver-api:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-impl:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-spi:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-supplier:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-transport-file:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-util:1.9.20")
  implementation("org.apache.maven:maven-resolver-provider:3.9.6")
}

// ////////////////////////////////////////////////////////////////
// Application Setup / Meta-data
application {
  mainClass = "org.ucombinator.jade.main.MainKt"
  applicationDefaultJvmArgs += listOf("-ea") // enable assertions
}

// version = "0.1.0" // Uncomment to manually set the version
apply<GitVersionPlugin>()

// ////////////////////////////////////////////////////////////////
// Code Formatting

// See https://github.com/saveourtool/diktat/blob/v2.0.0/diktat-gradle-plugin/src/main/kotlin/com/saveourtool/diktat/plugin/gradle/DiktatExtension.kt
//
// TODO: fix errors in stderr of diktatCheck:
//     line 1:3 no viable alternative at character '='
//     line 1:4 no viable alternative at character '='
//     line 1:5 no viable alternative at character '='
//     line 1:7 mismatched input 'null' expecting RPAREN
diktat {
  diktatConfigFile = rootProject.file("config/diktat/diktat-analysis.yml") // Avoid cluttering the root directory
  ignoreFailures = true
  // TODO: githubActions = true

  // See https://github.com/saveourtool/diktat/blob/v2.0.0/diktat-gradle-plugin/src/main/kotlin/com/saveourtool/diktat/plugin/gradle/extension/Reporters.kt
  reporters {
    plain()
    json()
    sarif()
    // gitHubActions()
    checkstyle()
    html()
  }
}

// See https://github.com/detekt/detekt/blob/v1.23.7/detekt-gradle-plugin/src/main/kotlin/io/gitlab/arturbosch/detekt/extensions/DetektExtension.kt
detekt {
  ignoreFailures = true
  allRules = true
  buildUponDefaultConfig = true
}

// See https://github.com/JLLeitschuh/ktlint-gradle/blob/v12.1.1/plugin/src/main/kotlin/org/jlleitschuh/gradle/ktlint/KtlintExtension.kt
ktlint {
  version = "1.4.1"
  verbose = true
  ignoreFailures = true
  enableExperimentalRules = true

  // See https://github.com/JLLeitschuh/ktlint-gradle/blob/v12.1.1/plugin/src/adapter/kotlin/org/jlleitschuh/gradle/ktlint/reporter/ReporterType.kt
  reporters {
    reporter(ReporterType.PLAIN)
    reporter(ReporterType.PLAIN_GROUP_BY_FILE)
    reporter(ReporterType.CHECKSTYLE)
    reporter(ReporterType.JSON)
    reporter(ReporterType.SARIF)
    reporter(ReporterType.HTML)
  }
}

// ////////////////////////////////////////////////////////////////
// Code Generation

fun generateSrc(fileName: String, code: String) {
  val generatedSrcDir = layout.buildDirectory.dir("generated/sources/jade/src/main/kotlin").get().getAsFile()
  generatedSrcDir.mkdirs()
  kotlin.sourceSets["main"].kotlin.srcDir(generatedSrcDir)
  val file = File(generatedSrcDir, fileName)
  file.writeText(code)
}

// TODO: Generate Flags.txt (see `sbt flagsTable`)
val generateClassfileFlags by tasks.registering {
  doLast {
    // TODO: avoid running when unchanged
    val flagsTxt = "src/main/kotlin/org/ucombinator/jade/classfile/Flags.txt"
    val code = GenerateClassfileFlags.code(File(projectDir, flagsTxt).readText(Charsets.UTF_8))
    generateSrc("Flags.kt", code)
  }
}

val generateBuildInfo by tasks.registering {
  doLast {
    // TODO: avoid running when unchanged
    // TODO: move to a plugin

    // project.kotlin.target.platformType
    // project.kotlin.target.targetName
    // project.kotlin.target.attributes
    // project.kotlin.target.name
    // project.sourceSets.main.get().runtimeClasspath

    fun String.escape() = """([\\\"\$])""".toRegex().replace(this, """\\$1""")

    val dependencies = project.configurations
      .flatMap { it.dependencies }
      .filterIsInstance<ExternalDependency>()
      .map {
        val key = "${it.group?.escape()}:${it.name.escape()}:${it.version?.escape()}"
        val value = "${it.targetConfiguration?.escape() ?: "default"}"
        "    \"${key}\" to \"${value}\","
      }.sorted()
      .joinToString("\n")

    val systemProperties = System.getProperties().toList()
      .filter { it.first.toString().matches("(java|os)\\..*".toRegex()) } // TODO: why filter?
      .map { "    \"${it.first?.toString()?.escape()}\" to \"${it.second?.toString()?.escape()}\"," }
      .sorted()
      .joinToString("\n")

    fun field(fieldName: String, value: Any?): String =
      "  val ${fieldName.trim()}: String? = ${value?.let { "\"${it.toString().escape()}\"" } ?: "null"}"

    val code = """
      |// Do not edit this file by hand.  It is generated by `gradle`.
      |package org.ucombinator.jade.main
      |
      |import javax.annotation.processing.Generated
      |
      |/** Information about the build and build-time environment */
      |@Generated("org.ucombinator.jade.gradle.GenerateBuildInformation")
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
      |
      |  /** Build-time dependencies and their target configurations */
      |  val dependencies: List<Pair<String, String>> = listOf(
      |$dependencies
      |  )
      |
      |  /** Build-time system properties and their values */
      |  val systemProperties: List<Pair<String, String>> = listOf(
      |$systemProperties
      |  )
      |
      |  val versionMessage = "${'$'}name version ${'$'}version (https://github.org/ucombinator/jade)"
      |}
      |
    """.trimMargin()

    generateSrc("BuildInformation.kt", code)
  }
}

// ////////////////////////////////////////////////////////////////
// Generic Configuration

// TODO: tasks.check.dependsOn(diktatCheck)
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
      includes.from("docs/Module.md")
    }
  }
}

listOf("runKtlintCheckOverMainSourceSet", "runKtlintCheckOverTestSourceSet").forEach { name ->
  tasks.named(name).configure {
    dependsOn(generateClassfileFlags)
    dependsOn(generateBuildInfo)
  }
}

// For why we have to fully qualify KotlinCompile see:
// https://stackoverflow.com/questions/55456176/unresolved-reference-compilekotlin-in-build-gradle-kts
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  // Avoid the warning: 'compileJava' task (current target is 11) and
  // 'compileKotlin' task (current target is 1.8) jvm target compatibility should
  // be set to the same Java version.
  kotlinOptions { jvmTarget = project.java.targetCompatibility.toString() }

  dependsOn(generateClassfileFlags)
  dependsOn(generateBuildInfo)
}
