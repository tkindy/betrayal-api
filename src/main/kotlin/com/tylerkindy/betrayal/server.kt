package com.tylerkindy.betrayal

import com.tylerkindy.betrayal.routes.gameRoutes
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.slf4j.event.Level
import java.time.Duration

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
                Json {
                    encodeDefaults = true
                    isLenient = true
                }
            )
        }
        install(CORS) {
            allowNonSimpleContentTypes = true
            host("localhost:3000")
            host(
                "herokuapp.com",
                schemes = listOf("https"),
                subDomains = listOf("betrayal-ui")
            )
        }
        install(CallLogging) {
            level = Level.INFO
        }
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(30)
        }
        install(StatusPages) {
            exception<IllegalArgumentException> {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to it.message)
                )
                throw it
            }
        }

        routing {
            gameRoutes()
        }
    }.start(wait = true)
}
