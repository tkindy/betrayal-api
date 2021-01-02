package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.RoomStackResponse
import com.tylerkindy.betrayal.db.advanceRoomStack
import com.tylerkindy.betrayal.db.flipRoom
import com.tylerkindy.betrayal.db.getRoomStackState
import com.tylerkindy.betrayal.db.placeRoom
import com.tylerkindy.betrayal.db.rotateFlipped
import com.tylerkindy.betrayal.sendUpdate
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

val roomStackRoutes: Route.() -> Unit = {
    route("roomStack") {
        get {
            val gameId = call.parameters["gameId"]!!
            call.respond(getRoomStackState(gameId))
        }

        post("skip") {
            val gameId = call.parameters["gameId"]!!
            call.respond(
                RoomStackResponse(
                    nextRoom = advanceRoomStack(gameId)
                )
            )
            sendUpdate(gameId)
        }

        post("flip") {
            val gameId = call.parameters["gameId"]!!
            call.respond(
                RoomStackResponse(
                    flippedRoom = flipRoom(gameId)
                )
            )
            sendUpdate(gameId)
        }

        post("rotate") {
            val gameId = call.parameters["gameId"]!!
            call.respond(
                RoomStackResponse(
                    flippedRoom = rotateFlipped(gameId)
                )
            )
            sendUpdate(gameId)
        }

        post("place") {
            val gameId = call.parameters["gameId"]!!
            val loc = call.receiveOrNull<GridLoc>()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Must pass grid location for room")
            call.respond(
                placeRoom(gameId, loc)
            )
            sendUpdate(gameId)
        }
    }
}
