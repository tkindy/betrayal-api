package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.Game
import com.tylerkindy.betrayal.GameRequest
import com.tylerkindy.betrayal.addUpdateRoute
import com.tylerkindy.betrayal.db.*
import com.tylerkindy.betrayal.gameUpdateManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

val gameRoutes: Routing.() -> Unit = {
    route("games") {
        route("{gameId}") {
            get {
                val gameId = call.parameters["gameId"]!!
                val game = transaction {
                    Games.select { Games.id eq gameId }
                        .firstOrNull()
                        ?.let {
                            Game(id = it[Games.id], name = it[Games.name])
                        }
                } ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(game)
            }
            roomRoutes()
            playerRoutes()
            monsterRoutes()
            roomStackRoutes()
            cardRoutes()
            diceRollRoutes()

            addUpdateRoute(gameUpdateManager, "gameId")
        }

        post {
            val gameRequest = call.receiveOrNull<GameRequest>()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "'name' and 'numPlayers' are required")

            if (gameRequest.name.length > 32) {
                return@post call.respond(HttpStatusCode.BadRequest, "Name can only be up to 32 characters")
            }
            if (!(3..6).contains(gameRequest.numPlayers)) {
                return@post call.respond(HttpStatusCode.BadRequest, "Must have between 3 and 6 players")
            }

            val gameId = buildGameId()

            transaction {
                Games.insert {
                    it[id] = gameId
                    it[name] = gameRequest.name
                }
            }

            insertStartingRooms(gameId)
            insertStartingPlayers(gameId, gameRequest.numPlayers)
            createRoomStack(gameId)
            createCardStacks(gameId)

            gameUpdateManager.sendUpdate(gameId)

            call.respond(Game(id = gameId, name = gameRequest.name))
        }
    }
}

private val gameIdCharacters = ('A'..'Z').toList()
private fun buildGameId(): String {
    return (0 until 6)
        .map { gameIdCharacters.random() }
        .fold(StringBuilder()) { builder, char -> builder.append(char) }
        .toString()
}
