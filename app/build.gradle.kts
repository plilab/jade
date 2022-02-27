import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.ucombinator.antlr.AntlrCharVocab

plugins {
  kotlin("jvm") // version "1.5.31"
  antlr // TODO: set generated/sources
  id("jade2.kotlin-application-conventions")
  id("org.jetbrains.dokka") version "1.5.31"
  // javadoc-plugin
  // kotlin-as-java-plugin
}

dependencies {
  // For parsing signatures
  antlr("org.antlr:antlr4:4.9.3")

  // Logging (see also io.github.microutils:kotlin-logging-jvm)
  implementation("ch.qos.logback:logback-classic:1.2.6")

  // Command-line argument parsing
  implementation("com.github.ajalt.clikt:clikt:3.4.0")

  // Abstract Syntax Trees for the Java language
  implementation("com.github.javaparser:javaparser-core:3.23.0") // Main library
  implementation("com.github.javaparser:javaparser-core-serialization:3.23.0") // Serialization to/from JSON
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.23.0") // Resolving symbols and identifiers
  // Omitting the JavaParser "parent" package as it is just metadata
  // Omitting the JavaParser "generator" and "metamodel" packages as they are just for building JavaParser

  // Logging (see also ch.qos.logback:logback-classic)
  implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")

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

  // TODO: doc
  implementation(project(":lib"))
}

// Avoid the warning: 'compileJava' task (current target is 11) and
// 'compileKotlin' task (current target is 1.8) jvm target compatibility should
// be set to the same Java version.
tasks.compileKotlin { kotlinOptions { jvmTarget = "11" } }

application {
  mainClass.set("org.ucombinator.jade.main.MainKt")
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

val antlrCharVocab = tasks.register<AntlrCharVocab>("antlrCharVocab", tasks.generateGrammarSource)
tasks.generateGrammarSource {
  dependsOn(antlrCharVocab)
}

val flagsGen by tasks.registering {
  doLast {
    // TODO: avoid running when unchanged
    val flagsCode = FlagsGen.code(File(projectDir, "src/main/kotlin/org/ucombinator/jade/classfile/Flags.txt").readText(Charsets.UTF_8))
    // TODO: use spotless for formatting
    // val scalafmt = org.scalafmt.interfaces.Scalafmt
      // .create(this.getClass.getClassLoader)
      // .withReporter(new ScalafmtSbtReporter(streamsValue.log, new java.io.OutputStreamWriter(streamsValue.binary()), true));
    // if (flagsCode != scalafmt.format(scalafmtConfig.value.toPath(), sourceFile.toPath(), flagsCode)) {
      // streamsValue.log.warn(f"\nGenerated file isn't formatted properly: ${sourceFile}\n\n")
    // }
    val generatedSrcDir = File(buildDir, "generated/sources/jade/src/main/kotlin")
    generatedSrcDir.mkdirs()
    val file = File(generatedSrcDir, "Flags.kt")
    file.writeText(flagsCode)
  }
}

tasks.withType<KotlinCompile<*>>() {
  dependsOn(tasks.generateGrammarSource)
  dependsOn(tasks.generateTestGrammarSource)
  dependsOn(flagsGen)
}
