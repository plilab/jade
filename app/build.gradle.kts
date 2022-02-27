import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.ucombinator.antlr.AntlrCharVocab

plugins {
  kotlin("jvm") // version "1.5.31"
  antlr
  id("jade2.kotlin-application-conventions")
  id("org.jetbrains.dokka") version "1.5.31"
  // javadoc-plugin
  // kotlin-as-java-plugin
}
// compileKotlin { kotlinOptions { jvmTarget = "1.8" } }

dependencies {
  // Grammars
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


  implementation(project(":lib"))
}

application {
  mainClass.set("org.ucombinator.jade.main.MainKt")
}

tasks.withType<KotlinCompile<*>>() {
  dependsOn(tasks.generateGrammarSource)
  dependsOn(tasks.generateTestGrammarSource)
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

tasks.register<AntlrCharVocab>("antlrCharVocab", tasks.generateGrammarSource)
tasks.generateGrammarSource {
  dependsOn(tasks.getByName("antlrCharVocab"))
}

tasks.register("genFlags") {
  doLast {
      // val streamsValue = streams.value
      // val sourceFile = flagsSourceFile.value
      // val flagsCode = FlagsGen.code(IO.read(flagsTableFile.value))
      val flagsCode = FlagsGen.code(File("/home/adamsmd/r/utah/jade/jade2/app/src/main/kotlin/org/ucombinator/jade/classfile/Flags.txt").readText(Charsets.UTF_8))
      println(flagsCode)
      // val scalafmt = org.scalafmt.interfaces.Scalafmt
        // .create(this.getClass.getClassLoader)
        // .withReporter(new ScalafmtSbtReporter(streamsValue.log, new java.io.OutputStreamWriter(streamsValue.binary()), true));
      // if (flagsCode != scalafmt.format(scalafmtConfig.value.toPath(), sourceFile.toPath(), flagsCode)) {
        // streamsValue.log.warn(f"\nGenerated file isn't formatted properly: ${sourceFile}\n\n")
      // }
      // IO.write(sourceFile, flagsCode)
      // Seq(sourceFile)
  }
}

