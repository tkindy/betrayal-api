package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.db.getRooms
import com.tylerkindy.betrayal.db.moveRoom
import com.tylerkindy.betrayal.db.rotateRoom
import com.tylerkindy.betrayal.sendUpdate
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

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

            post("rotate") {
                val gameId = call.parameters["gameId"]!!
                val roomId = call.parameters["roomId"]?.toInt()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid room ID")

                rotateRoom(gameId, roomId)
                call.respond(HttpStatusCode.OK, "")
                sendUpdate(gameId)
            }
        }
    }
}
