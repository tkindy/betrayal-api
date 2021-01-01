package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.db.getAllPlayerInventories
import com.tylerkindy.betrayal.db.getPlayers
import com.tylerkindy.betrayal.db.movePlayer
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

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
        }

        get("inventories") {
            val gameId = call.parameters["gameId"]!!
            call.respond(getAllPlayerInventories(gameId))
        }
    }
}
