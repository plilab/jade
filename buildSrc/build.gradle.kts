repositories {
  gradlePluginPortal()
}

plugins {
  kotlin("jvm") version "1.5.31"
  `kotlin-dsl` version "2.1.7"

  id("com.diffplug.spotless") version "6.3.0" // Adds: ./gradlew -p buildSrc spotlessCheck

  id("com.github.ben-manes.versions") version "0.42.0" // Adds: ./gradlew -p buildSrc dependencyUpdates
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin") // :1.5.31

  // Git API (for `GitVersionsPlugin.kt`)
  implementation("org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r")

  // HTML parsing (for `GenerateClassfileFlags.table()`)
  implementation("org.jsoup:jsoup:1.14.3")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    // ktfmt()
    ktlint().userData(
      mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2"
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
