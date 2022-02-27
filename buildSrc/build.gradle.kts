plugins {
  kotlin("jvm") version "1.5.31"

  // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
  `kotlin-dsl` version "2.1.7"
}

repositories {
  // Use the plugin portal to apply community plugins in convention plugins.
  gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin") // :1.5.31

  // HTML parsing (for `FlagsGen.table()`)
  implementation("org.jsoup:jsoup:1.14.3")
}
