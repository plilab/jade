plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Depend on the main project
    implementation(project(":"))
    implementation("com.github.javaparser:javaparser-core:3.25.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    // Add ASM dependencies explicitly for the playground
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-analysis:9.7.1")
    implementation("org.ow2.asm:asm-commons:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
    implementation("org.ow2.asm:asm-util:9.7.1")

    // Add testing libraries from the root project for consistency
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

application {
    mainClass = "org.ucombinator.jade.playground.cli.PlaygroundMainKt"
    applicationDefaultJvmArgs += listOf("-ea") // enable assertions, same as root project
}

// Enable stdin for the run task
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.withType<Test> {
    useJUnitPlatform()
}