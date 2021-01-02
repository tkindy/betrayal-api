import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
    kotlin("plugin.serialization") version "1.4.20"
    application
    id("org.jmailen.kotlinter") version "3.3.0"
    id("com.github.ben-manes.versions") version "0.36.0"
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

val ktorVersion = "1.4.2"
val postgresVersion = "42.2.18"
val exposedVersion = "0.28.1"
val kotestVersion = "4.3.1"

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.apache.commons:commons-csv:1.8")

    liquibaseRuntime("org.liquibase:liquibase-core:4.2.0")
    liquibaseRuntime("org.postgresql:postgresql:$postgresVersion")
    liquibaseRuntime("javax.xml.bind:jaxb-api:2.3.1")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

application {
    mainClass.set("com.tylerkindy.betrayal.ServerKt")
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
}

tasks.register("stage") {
    dependsOn("installDist")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
