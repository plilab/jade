repositories {
  gradlePluginPortal()
}

plugins {
  kotlin("jvm") version "1.9.22"
  `kotlin-dsl` // version built into gradle

  id("com.github.ben-manes.versions") version "0.51.0" // Adds: ./gradlew -p buildSrc dependencyUpdates
  id("com.saveourtool.diktat") version "2.0.0" // Adds: ./gradlew -p buildSrc diktatCheck
  id("io.gitlab.arturbosch.detekt") version "1.23.6" // Adds: ./gradlew -p buildSrc detekt
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1" // Adds: ./gradlew -p buildSrc ktlintCheck
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin") // version matches kotlin("jvm")

  // Git API (for `GitVersionsPlugin.kt`)
  implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")

  // HTML parsing (for `GenerateClassfileFlags.table()`)
  implementation("org.jsoup:jsoup:1.17.2")
}

// ////////////////////////////////////////////////////////////////
// Code Formatting

// See https://github.com/saveourtool/diktat/blob/v2.0.0/diktat-gradle-plugin/src/main/kotlin/com/saveourtool/diktat/plugin/gradle/DiktatExtension.kt
diktat {
  ignoreFailures = true
  diktatConfigFile = File("../diktat-analysis.yml")
}

// See https://github.com/detekt/detekt/blob/v1.23.6/detekt-gradle-plugin/src/main/kotlin/io/gitlab/arturbosch/detekt/extensions/DetektExtension.kt
detekt {
  ignoreFailures = true
  allRules = true
  buildUponDefaultConfig = true
}

// See https://github.com/JLLeitschuh/ktlint-gradle/blob/v12.1.1/plugin/src/main/kotlin/org/jlleitschuh/gradle/ktlint/KtlintExtension.kt
ktlint {
  version = "1.2.1"
  verbose = true
  ignoreFailures = true
  enableExperimentalRules = true
}
