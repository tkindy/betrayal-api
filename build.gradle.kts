import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintFormatTask

plugins {
    kotlin("jvm") version "1.4.10"
    application
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}
group = "com.tylerkindy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/ktor")
    }
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlinx")
    }
}

val ktorVersion = "1.4.1"

dependencies {
    testImplementation(kotlin("test-junit5"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
}

application {
    mainClassName = "com.tylerkindy.betrayal.ServerKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    dependsOn(tasks.withType<KtlintFormatTask>())
}

tasks.register("stage") {
    dependsOn("installDist")
}
