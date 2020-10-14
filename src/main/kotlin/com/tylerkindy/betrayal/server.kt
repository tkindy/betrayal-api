package com.tylerkindy.betrayal

import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.DefaultJson
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

fun main() {
    Database.connect(
        HikariDataSource().apply {
            jdbcUrl = System.getenv("JDBC_DATABASE_URL")
        }
    )
    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(
                Json(DefaultJson) {
                    prettyPrint = true
                }
            )
        }

        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
            get("random/{min}/{max}") {
                val min = call.parameters["min"]?.toIntOrNull() ?: 0
                val max = call.parameters["max"]?.toIntOrNull() ?: 10
                val randomString = "${(min until max).shuffled().last()}"
                call.respond(mapOf("value" to randomString))
            }
            gameRoutes()
        }
    }.start(wait = true)
}
