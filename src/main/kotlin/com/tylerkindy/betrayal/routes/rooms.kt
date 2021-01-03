package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.db.getRooms
import com.tylerkindy.betrayal.db.moveRoom
import com.tylerkindy.betrayal.sendUpdate
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

val roomRoutes: Route.() -> Unit = {
    route("rooms") {
        get {
            val gameId = call.parameters["gameId"]!!
            return@get call.respond(getRooms(gameId))
        }

        route("{roomId}") {
            post("move") {
                val gameId = call.parameters["gameId"]!!
                val roomId = call.parameters["roomId"]?.toInt()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid room ID")
                val loc = call.receiveOrNull<GridLoc>()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing gridX or gridY")

                moveRoom(gameId, roomId, loc)
                call.respond(HttpStatusCode.OK, "")
                sendUpdate(gameId)
            }
        }
    }
}
