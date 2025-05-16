// NOTE: Groups with comment headers are sorted alphabetically by group name (TODO)

// TODO: publish to maven central and gradle plugins

group = "org.ucombinator.jade"
// `name = ...` is in settings.gradle.kts because it is read-only here
// version = "0.1.0" // Uncomment to manually set the version (see GitVersionPlugin)
description = "A Java decompiler that aims for high reliability through extensive testing."

repositories {
  mavenCentral()
}

// To see a complete list of tasks, use: ./gradlew tasks
plugins {
  kotlin("jvm") // version set by buildSrc/build.gradle.kts
  // TODO: kotlin("plugin.power-assert") version "2.0.0" // See https://kotlinlang.org/docs/power-assert.html
  application // Provides "./gradlew installDist" then "./build/install/jade/bin/jade"

  // Documentation
  id("org.jetbrains.dokka") version "1.9.20" // Adds: ./gradlew dokka{Gfm,Html,Javadoc,Jekyll}

  // Linting and Code Formatting
  // id("com.ncorti.ktfmt.gradle") version "0.21.0" // Adds: ./gradlew ktfmtCheck (omit because issues errors not warnings)
  id("com.saveourtool.diktat") version "2.0.0" // Adds: ./gradlew diktatCheck
  id("io.gitlab.arturbosch.detekt") version "1.23.7" // Adds: ./gradlew detekt
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1" // Adds: ./gradlew ktlintCheck
  id("se.solrike.sonarlint") version "2.1.0" // Tasks: sonarlint{Main,Test} (omit because issues errors not warnings)

  // Code Coverage
  id("jacoco") // version built into Gradle // Adds: ./gradlew jacocoTestReport
  id("org.jetbrains.kotlinx.kover") version "0.8.3" // Adds: ./gradlew koverMergedHtmlReport

  // Dependency Versions and Licenses
  id("com.github.ben-manes.versions") version "0.51.0" // Adds: ./gradlew dependencyUpdates
  id("com.github.jk1.dependency-license-report") version "2.9" // Adds: ./gradlew generateLicenseReport

  // Local Plugins
  id("org.michaeldadams.gradle.generate-build-information")
  id("org.michaeldadams.gradle.git-version")
  id("org.michaeldadams.gradle.version-task")
}
apply<org.michaeldadams.gradle.GenerateClassfileFlagsPlugin>()

dependencies {
  // Testing
  testImplementation(kotlin("test"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")

  // Linting and Code Formatting
  // detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7") // We use org.jlleitschuh.gradle.ktlint instead to use the newest ktlint
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.7")
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.7")
  sonarlintPlugins("org.sonarsource.kotlin:sonar-kotlin-plugin:2.13.0.2116") // TODO: others

  // Logging
  implementation("ch.qos.logback:logback-classic:1.5.12")
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

  // Command-line argument parsing
  implementation("com.github.ajalt.clikt:clikt:5.0.1")
  implementation("com.github.ajalt.clikt:clikt-markdown:5.0.1")

  // Java source abstract syntax trees
  implementation("com.github.javaparser:javaparser-core:3.26.2") // Main library
  implementation("com.github.javaparser:javaparser-core-serialization:3.26.2") // Serialization to/from JSON
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.2") // Resolving symbols and identifiers
  // Omitting the JavaParser "parent" package as it is just metadata
  // Omitting the JavaParser "generator" and "metamodel" packages as they are just for building JavaParser

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

  // JVM Bytecode / Class files
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

generateBuildInformation {
  packageName = "org.ucombinator.jade.main"
  url = "https://github.com/adamsmd/jade"
}

// ////////////////////////////////////////////////////////////////
// Application Setup and Meta-data
application {
  mainClass = "org.ucombinator.jade.main.MainKt"
  applicationDefaultJvmArgs += listOf("-ea") // enable assertions
}

// ////////////////////////////////////////////////////////////////
// Linting and Code Formatting

// TODO: fix errors in stderr of diktatCheck:
//     line 1:3 no viable alternative at character '='
//     line 1:4 no viable alternative at character '='
//     line 1:5 no viable alternative at character '='
//     line 1:7 mismatched input 'null' expecting RPAREN
//
// See https://github.com/saveourtool/diktat/blob/v2.0.0/diktat-gradle-plugin/src/main/kotlin/com/saveourtool/diktat/plugin/gradle/DiktatExtension.kt
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
    org.jlleitschuh.gradle.ktlint.reporter.ReporterType.let {
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN_GROUP_BY_FILE)
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.JSON)
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
  }
}

// See https://github.com/Lucas3oo/sonarlint-gradle-plugin/blob/d738512fb481114cf9fcbc62126460113c289de7/src/main/java/se/solrike/sonarlint/SonarlintExtension.java
sonarlint {
  // TODO
  ignoreFailures = true
}

// ////////////////////////////////////////////////////////////////
// Generic Configuration

// TODO: tasks.check.dependsOn(diktatCheck)
// TODO: task: reports
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
    dependsOn("generateClassfileFlags")
    dependsOn("generateBuildInformation")
  }
}

// For why we have to fully qualify KotlinCompile see:
// https://stackoverflow.com/questions/55456176/unresolved-reference-compilekotlin-in-build-gradle-kts
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  // Avoid the warning: 'compileJava' task (current target is 11) and
  // 'compileKotlin' task (current target is 1.8) jvm target compatibility should
  // be set to the same Java version.
  // kotlinOptions { jvmTarget = project.java.targetCompatibility.toString() }

  dependsOn("generateClassfileFlags") // TODO: put in plugin
  dependsOn("generateBuildInformation")
}
