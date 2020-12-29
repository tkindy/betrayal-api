package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.db.drawItem
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route

val cardRoutes: Route.() -> Unit = {
    route("cards") {
        route("items") {
            post("draw") {
                val gameId = call.parameters["gameId"]!!
                call.respond(drawItem(gameId))
            }
        }
    }
}
