package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.db.discardHeldCard
import com.tylerkindy.betrayal.db.getPlayer
import com.tylerkindy.betrayal.db.getPlayers
import com.tylerkindy.betrayal.db.giveHeldCardToPlayer
import com.tylerkindy.betrayal.db.movePlayer
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.serialization.Serializable

val playerRoutes: Route.() -> Unit = {
    route("players") {
        get {
            val gameId = call.parameters["gameId"]!!
            return@get call.respond(getPlayers(gameId))
        }

        route("{playerId}") {
            post("move") {
                val gameId = call.parameters["gameId"]!!
                val playerId = call.parameters["playerId"]!!.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid player ID")
                val loc = call.receiveOrNull<GridLoc>()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Must specify new location")

                movePlayer(gameId, playerId, loc)
                call.respond(HttpStatusCode.OK)
            }

            route("cards/{cardId}") {
                post("discard") {
                    val gameId = call.parameters["gameId"]!!
                    val playerId = call.parameters["playerId"]!!.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid player ID")
                    val cardId = call.parameters["cardId"]!!.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid card ID")

                    discardHeldCard(gameId, playerId, cardId)
                    call.respond(getPlayer(gameId, playerId))
                }
                post("giveToPlayer") {
                    val gameId = call.parameters["gameId"]!!
                    val fromPlayerId = call.parameters["playerId"]!!.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid player ID")
                    val cardId = call.parameters["cardId"]!!.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid card ID")
                    val toPlayerId = call.receiveOrNull<GiveHeldCardToPlayerBody>()?.toPlayerId
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing toPlayerId")

                    giveHeldCardToPlayer(gameId, fromPlayerId, cardId, toPlayerId)
                    call.respond(getPlayers(gameId))
                }
            }
        }
    }
}

@Serializable
data class GiveHeldCardToPlayerBody(val toPlayerId: Int)
