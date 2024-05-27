import org.ucombinator.jade.gradle.GenerateClassfileFlags
import org.ucombinator.jade.gradle.GitVersionPlugin
import java.text.SimpleDateFormat
import java.util.Date

repositories {
  mavenCentral()
}

plugins {
  kotlin("jvm") // version matches buildSrc/build.gradle.kts
  application

  // Documentation
  id("org.jetbrains.dokka") version "1.9.20" // Adds: ./gradlew dokka{Gfm,Html,Javadoc,Jekyll}

  // Code Formatting
  id("com.saveourtool.diktat") version "2.0.0" // Adds: ./gradlew diktatCheck
  id("io.gitlab.arturbosch.detekt") version "1.23.6" // Adds: ./gradlew detekt
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1" // Adds: ./gradlew ktlintCheck (TODO: requires disabling diktat)

  // Code Coverage
  id("jacoco") // version built into Gradle // Adds: ./gradlew jacocoTestReport
  id("org.jetbrains.kotlinx.kover") version "0.8.0" // Adds: ./gradlew koverMergedHtmlReport

  // Dependency Versions and Licenses
  id("com.github.ben-manes.versions") version "0.51.0" // Adds: ./gradlew dependencyUpdates
  id("com.github.jk1.dependency-license-report") version "2.7" // Adds: ./gradlew generateLicenseReport
}

dependencies {
  // Testing
  testImplementation(kotlin("test"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

  // detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6") // We use org.jlleitschuh.gradle.ktlint instead to use the newest ktlint
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.6")
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.6")

  // NOTE: these are sorted alphabetically

  // Logging (see also io.github.microutils:kotlin-logging-jvm)
  implementation("ch.qos.logback:logback-classic:1.5.6")

  // Command-line argument parsing
  implementation("com.github.ajalt.clikt:clikt:4.4.0")

  // Abstract Syntax Trees for the Java language
  implementation("com.github.javaparser:javaparser-core:3.25.10") // Main library
  implementation("com.github.javaparser:javaparser-core-serialization:3.25.10") // Serialization to/from JSON
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.10") // Resolving symbols and identifiers
  // Omitting the JavaParser "parent" package as it is just metadata
  // Omitting the JavaParser "generator" and "metamodel" packages as they are just for building JavaParser

  // Google Cloud Storage (for accessing the Maven mirror)
  implementation(platform("com.google.cloud:libraries-bom:26.39.0"))
  implementation("com.google.cloud:google-cloud-storage:2.38.0")

  // Logging (see also ch.qos.logback:logback-classic)
  implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

  // Compressed files
  implementation("org.apache.commons:commons-compress:1.26.1")

  // For parallelizing access to Google Cloud Storage
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

  // Vertex and edge graphs
  implementation("org.jgrapht:jgrapht-core:1.5.2")
  implementation("org.jgrapht:jgrapht-ext:1.5.2")
  // implementation("org.jgrapht:jgrapht-guava:1.5.2")
  implementation("org.jgrapht:jgrapht-io:1.5.2")
  implementation("org.jgrapht:jgrapht-opt:1.5.2")

  // `.class` file parsing and analysis
  implementation("org.ow2.asm:asm:9.7")
  implementation("org.ow2.asm:asm-analysis:9.7")
  implementation("org.ow2.asm:asm-commons:9.7")
  // implementation("org.ow2.asm:asm-test:9.7")
  implementation("org.ow2.asm:asm-tree:9.7")
  implementation("org.ow2.asm:asm-util:9.7")

  // Maven
  implementation("org.apache.maven.resolver:maven-resolver-api:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-spi:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-util:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-impl:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-transport-file:1.9.20")
  implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.9.20")
  implementation("org.apache.maven:maven-resolver-provider:3.9.6")
}

// ////////////////////////////////////////////////////////////////
// Application Setup / Meta-data
application {
  mainClass.set("org.ucombinator.jade.main.MainKt")
}

// version = "0.1.0" // Uncomment to manually set the version
apply<GitVersionPlugin>()

// ////////////////////////////////////////////////////////////////
// Code Formatting

// https://github.com/jlleitschuh/ktlint-gradle/blob/master/plugin/src/main/kotlin/org/jlleitschuh/gradle/ktlint/KtlintExtension.kt
ktlint {
  version.set("1.2.1")
  verbose.set(true)
  ignoreFailures.set(true)
  enableExperimentalRules.set(true)
  filter {
    exclude { it.file.path.startsWith("$buildDir" + File.separator) } // Avoid generated files
  }
}

// https://github.com/analysis-dev/diktat/blob/master/diktat-gradle-plugin/src/main/kotlin/org/cqfn/diktat/plugin/gradle/DiktatExtension.kt
diktat {
  ignoreFailures = true
}

// https://github.com/detekt/detekt/blob/main/detekt-gradle-plugin/src/main/kotlin/io/gitlab/arturbosch/detekt/extensions/DetektExtension.kt
detekt {
  ignoreFailures = true
  buildUponDefaultConfig = true
  allRules = true
}

// ////////////////////////////////////////////////////////////////
// Code Generation

fun generateSrc(fileName: String, code: String) {
  val generatedSrcDir = File(buildDir, "generated/sources/jade/src/main/kotlin") // TODO: move to generated-src
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

// ////////////////////////////////////////////////////////////////
// Generic Configuration

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
