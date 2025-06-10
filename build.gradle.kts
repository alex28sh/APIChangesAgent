plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"
var toolingApiVersion = gradle.gradleVersion

repositories {
    mavenCentral()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Koog agents library
    implementation("ai.koog:koog-agents:0.2.0")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
//
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("ch.qos.logback:logback-classic:1.4.11") // or latest
//    // File operations and utilities
//    implementation("commons-io:commons-io:2.13.0")
//
//    // Gradle tooling API for running Gradle builds
////    implementation("org.gradle:gradle-tooling-api:8.4")
//
//    // Spring framework for testing
//    implementation("org.springframework:spring-context:6.0.19")


//    implementation("org.gradle:gradle-tooling-api:${toolingApiVersion}")
    // The tooling API need an SLF4J implementation available at runtime, replace this with any other implementation
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

    implementation("me.tongfei:progressbar:0.9.5")
    // Testing
    testImplementation(kotlin("test"))
//    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

// Configure the application plugin
application {
    mainClass.set("org.example.MainKt")
}
