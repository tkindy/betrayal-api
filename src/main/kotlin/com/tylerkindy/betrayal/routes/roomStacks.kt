package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.RoomStackResponse
import com.tylerkindy.betrayal.db.*
import com.tylerkindy.betrayal.sendUpdate
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

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
            rotateFlipped(gameId)
            call.respond(getRoomStackState(gameId))
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
