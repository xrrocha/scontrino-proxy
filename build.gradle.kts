import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "scontrino"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainModule.set("scontrino.proxy")
    mainClass.set("scontrino.proxy.MainKt")
}

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.0")
    testImplementation(kotlin("test"))
    testImplementation("com.h2database:h2:2.1.214")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
