import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  // kotlin("jvm") // version "1.5.31"
  antlr
  id("jade2.kotlin-application-conventions")
  id("org.jetbrains.dokka") version "1.5.31"
  // javadoc-plugin
  // kotlin-as-java-plugin
}

dependencies {
  // Grammars
  antlr("org.antlr:antlr4:4.9.3")

  // Command-line argument parsing
  implementation("com.github.ajalt.clikt:clikt:3.4.0")

  // Abstract Syntax Trees for the Java language
  implementation("com.github.javaparser:javaparser-core:3.23.0") // Main library
  implementation("com.github.javaparser:javaparser-core-serialization:3.23.0") // Serialization to/from JSON
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.23.0") // Resolving symbols and identifiers
  // Omitting the JavaParser "parent" package as it is just metadata
  // Omitting the JavaParser "generator" and "metamodel" packages as they are just for building JavaParser

  // Logging
  implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")
  // TODO: https://muthuraj57.medium.com/logging-in-kotlin-the-right-way-d7a357bb0343

  // Vertex and edge graphs
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("org.jgrapht:jgrapht-ext:1.5.1")
  //implementation("org.jgrapht:jgrapht-guava:1.5.1")
  implementation("org.jgrapht:jgrapht-io:1.5.1")
  implementation("org.jgrapht:jgrapht-opt:1.5.1")

  // `.class` file parsing and analysis
  implementation("org.ow2.asm:asm:9.2")
  implementation("org.ow2.asm:asm-analysis:9.2")
  implementation("org.ow2.asm:asm-commons:9.2")
  //implementation("org.ow2.asm:asm-test:9.2")
  implementation("org.ow2.asm:asm-tree:9.2")
  implementation("org.ow2.asm:asm-util:9.2")

  // Testing
  testImplementation(kotlin("test"))


  implementation(project(":lib"))
}

application {
    mainClass.set("jade2.app.AppKt")
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            includes.from("Module.md")
        }
    }
}

tasks.withType<Test> {
  this.testLogging {
      this.showStandardStreams = true
  }
}