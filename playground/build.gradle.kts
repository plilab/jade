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

    // Add testing libraries from the root project for consistency
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

application {
    mainClass = "org.ucombinator.jade.playground.PlaygroundKt"
    applicationDefaultJvmArgs += listOf("-ea") // enable assertions, same as root project
}

tasks.withType<Test> {
    useJUnitPlatform()
} 