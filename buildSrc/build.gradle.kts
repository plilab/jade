// NOTE: Groups with comment headers are sorted alphabetically by group name

repositories {
  gradlePluginPortal()
}

plugins {
  kotlin("jvm") version "2.0.20"
  `kotlin-dsl` // version built into gradle

  id("com.github.ben-manes.versions") version "0.51.0" // Adds: ./gradlew -p buildSrc dependencyUpdates
  id("com.saveourtool.diktat") version "2.0.0" // Adds: ./gradlew -p buildSrc diktatCheck
  id("io.gitlab.arturbosch.detekt") version "1.23.7" // Adds: ./gradlew -p buildSrc detekt
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1" // Adds: ./gradlew -p buildSrc ktlintCheck
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin") // version set by kotlin("jvm")

  // Linting and Code Formatting
  // Note that ktlint must match the version in bibscrape-gradle-settings.gradle.kts
  // Used by defined rules
  implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.4.1")
  implementation("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.4.1")
  implementation("com.pinterest.ktlint:ktlint-rule-engine-core:1.4.1")

  // Git API (for `GitVersionsPlugin.kt`)
  implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

  // HTML parsing (for `GenerateClassfileFlags.table()`)
  implementation("org.jsoup:jsoup:1.18.1")
}

// Prevent warning: :jar: No valid plugin descriptors were found in META-INF/gradle-plugins
gradlePlugin {
  plugins {
    create("generateBuildInformationPlugin") {
      displayName = "Generate Build Information Plugin"
      description = "A plugin that creates a BuildInformation class containing information about the build and build-time environment"
      id = "org.michaeldadams.gradle.generate-build-information"
      implementationClass = "org.michaeldadams.gradle.GenerateBuildInformationPlugin"
    }
    create("gitVersionPlugin") {
      displayName = "Git Version Plugin"
      description = "A plugin that sets the project version based on Git tags"
      id = "org.michaeldadams.gradle.git-version"
      implementationClass = "org.michaeldadams.gradle.GitVersionPlugin"
    }
    create("versionTaskPlugin") {
      displayName = "Version-Task Plugin"
      description = "A plugin that adds a 'version' task that displays the project version"
      id = "org.michaeldadams.gradle.version-task"
      implementationClass = "org.michaeldadams.gradle.VersionTaskPlugin"
    }
  }
}

// ////////////////////////////////////////////////////////////////
// Code Formatting

// See https://github.com/saveourtool/diktat/blob/v2.0.0/diktat-gradle-plugin/src/main/kotlin/com/saveourtool/diktat/plugin/gradle/DiktatExtension.kt
diktat {
  diktatConfigFile = rootProject.file("config/diktat/diktat-analysis.yml") // Avoid cluttering the root directory
  ignoreFailures = true
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
}
