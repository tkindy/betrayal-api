package com.tylerkindy.betrayal

import com.tylerkindy.betrayal.routes.gameRoutes
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.slf4j.event.Level

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

        routing {
            gameRoutes()
        }
    }.start(wait = true)
}
