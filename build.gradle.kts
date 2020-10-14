import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintFormatTask

plugins {
    kotlin("jvm") version "1.4.10"
    application
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("com.github.ben-manes.versions") version "0.33.0"
    id("org.liquibase.gradle") version "2.0.4"
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
val postgresVersion = "42.2.17"
val exposedVersion = "0.25.1"

dependencies {
    testImplementation(kotlin("test-junit5"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:3.4.5")

    liquibaseRuntime("org.liquibase:liquibase-core:3.8.1")
    liquibaseRuntime("org.postgresql:postgresql:$postgresVersion")
    liquibaseRuntime("javax.xml.bind:jaxb-api:2.3.1")
}

application {
    mainClassName = "com.tylerkindy.betrayal.ServerKt"
}

liquibase {
    val url = System.getenv("JDBC_DATABASE_URL")

    activities.register("main") {
        arguments = mapOf(
            "changeLogFile" to "src/main/resources/migrations.sql",
            "url" to url
        )
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    dependsOn(tasks.withType<KtlintFormatTask>())
}

tasks.register("stage") {
    dependsOn("installDist")
}
