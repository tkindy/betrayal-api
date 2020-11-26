package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.db.getPlayers
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

val playerRoutes: Route.() -> Unit = {
    route("players") {
        get {
            val gameId = call.parameters["gameId"]!!
            return@get call.respond(getPlayers(gameId))
        }
    }
}
