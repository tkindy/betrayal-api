package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.RoomStackResponse
import com.tylerkindy.betrayal.db.advanceRoomStack
import com.tylerkindy.betrayal.db.flipRoom
import com.tylerkindy.betrayal.db.getRoomStackState
import com.tylerkindy.betrayal.db.rotateFlipped
import io.ktor.application.call
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
        }

        post("flip") {
            val gameId = call.parameters["gameId"]!!
            call.respond(
                RoomStackResponse(
                    flippedRoom = flipRoom(gameId)
                )
            )
        }

        post("rotate") {
            val gameId = call.parameters["gameId"]!!
            call.respond(
                RoomStackResponse(
                    flippedRoom = rotateFlipped(gameId)
                )
            )
        }
    }
}
