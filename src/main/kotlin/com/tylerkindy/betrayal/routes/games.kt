package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.Game
import com.tylerkindy.betrayal.GameRequest
import com.tylerkindy.betrayal.db.Games
import com.tylerkindy.betrayal.db.createCardStacks
import com.tylerkindy.betrayal.db.createRoomStack
import com.tylerkindy.betrayal.db.insertStartingPlayers
import com.tylerkindy.betrayal.db.insertStartingRooms
import com.tylerkindy.betrayal.getUpdates
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.websocket.webSocket
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            roomStackRoutes()
            cardRoutes()

            webSocket {
                val gameId = call.parameters["gameId"]!!
                getUpdates(gameId).collect { update ->
                    send(Frame.Text(Json.encodeToString(update)))
                }
            }
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
