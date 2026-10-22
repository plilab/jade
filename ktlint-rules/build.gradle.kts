plugins {
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  // Ktlint supplies these at runtime through its isolated worker classpath.
  compileOnly("com.pinterest.ktlint:ktlint-ruleset-standard:1.8.0")
  compileOnly("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.8.0")
  compileOnly("com.pinterest.ktlint:ktlint-rule-engine-core:1.8.0")
}
