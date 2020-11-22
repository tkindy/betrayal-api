package com.tylerkindy.betrayal

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

val gameIdCharacters = ('A'..'Z').toList()

val gameRoutes: Routing.() -> Unit = {
    route("games") {
        get("{id}") {
            val id = call.parameters["id"]!!
            val game = transaction {
                Games.select { Games.id eq id }
                    .firstOrNull()
                    ?.let { Game(id = it[Games.id], name = it[Games.name]) }
            }

            if (game == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(game)
        }

        post {
            val gameRequest = call.receiveOrNull<GameRequest>()

            if (gameRequest == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing 'name'")
                return@post
            }
            if (gameRequest.name.length > 32) {
                call.respond(HttpStatusCode.BadRequest, "Name can only be up to 32 characters")
                return@post
            }

            val gameId = buildGameId()
            transaction {
                Games.insert {
                    it[id] = gameId
                    it[name] = gameRequest.name
                }
            }

            call.respond(Game(id = gameId, name = gameRequest.name))
        }
    }
}

fun buildGameId(): String {
    return (0 until 6)
        .map { gameIdCharacters.random() }
        .fold(StringBuilder()) { builder, char -> builder.append(char) }
        .toString()
}

@Serializable
data class GameRequest(val name: String)

@Serializable
data class Game(val id: String, val name: String)
