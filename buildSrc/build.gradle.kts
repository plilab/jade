repositories {
  gradlePluginPortal()
}

plugins {
  kotlin("jvm") version "1.5.31"
  `kotlin-dsl` version "2.1.7"

  id("com.github.ben-manes.versions") version "0.42.0" // Adds: ./gradlew -p buildSrc dependencyUpdates
  id("io.gitlab.arturbosch.detekt").version("1.19.0") // Adds: ./gradlew -p buildSrc detekt
  // id("org.cqfn.diktat.diktat-gradle-plugin") version "1.0.3" // Adds: ./gradlew -p buildSrc diktatCheck
  id("org.jlleitschuh.gradle.ktlint") version "10.2.1" // Adds: ./gradlew -p buildSrc ktlintCheck (requires disabling diktat)
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin") // :1.5.31

  // Git API (for `GitVersionsPlugin.kt`)
  implementation("org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r")

  // HTML parsing (for `GenerateClassfileFlags.table()`)
  implementation("org.jsoup:jsoup:1.14.3")
}

// ////////////////////////////////////////////////////////////////
// Code Formatting

// https://github.com/jlleitschuh/ktlint-gradle/blob/master/plugin/src/main/kotlin/org/jlleitschuh/gradle/ktlint/KtlintExtension.kt
ktlint {
  verbose.set(true)
  ignoreFailures.set(true)
  enableExperimentalRules.set(true)
  disabledRules.set(setOf())
}

// // https://github.com/analysis-dev/diktat/blob/master/diktat-gradle-plugin/src/main/kotlin/org/cqfn/diktat/plugin/gradle/DiktatExtension.kt
// diktat {
//   ignoreFailures = true
//   diktatConfigFile = File("../diktat-analysis.yml")
// }

// https://github.com/detekt/detekt/blob/main/detekt-gradle-plugin/src/main/kotlin/io/gitlab/arturbosch/detekt/extensions/DetektExtension.kt
detekt {
  ignoreFailures = true
  buildUponDefaultConfig = true
  allRules = true
}
