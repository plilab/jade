repositories {
  gradlePluginPortal()
}

plugins {
  kotlin("jvm") version "1.5.31"
  `kotlin-dsl` version "2.1.7"

  id("com.github.ben-manes.versions") version "0.42.0" // Adds: ./gradlew -p buildSrc dependencyUpdates
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin") // :1.5.31

  // For GitVersions.kt
  implementation("org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r")

  // HTML parsing (for `GenerateClassfileFlags.table()`)
  implementation("org.jsoup:jsoup:1.14.3")
}
