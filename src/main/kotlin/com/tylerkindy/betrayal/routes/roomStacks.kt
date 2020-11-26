package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.RoomStackResponse
import com.tylerkindy.betrayal.db.getStackRoom
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

val roomStackRoutes: Route.() -> Unit = {
    route("roomStack") {
        get {
            val gameId = call.parameters["gameId"]!!
            call.respond(
                RoomStackResponse(
                    nextRoom = getStackRoom(gameId)
                )
            )
        }
    }
}
