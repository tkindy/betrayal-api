package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.db.addMonster
import com.tylerkindy.betrayal.db.entranceHallLoc
import com.tylerkindy.betrayal.db.getMonsters
import com.tylerkindy.betrayal.db.moveMonster
import com.tylerkindy.betrayal.sendUpdate
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

val monsterRoutes: Route.() -> Unit = {
    route("monsters") {
        get {
            val gameId = call.parameters["gameId"]!!
            call.respond(getMonsters(gameId))
        }

        post {
            val gameId = call.parameters["gameId"]!!
            call.respond(addMonster(gameId, entranceHallLoc))
            sendUpdate(gameId)
        }

        route("{monsterId}") {
            post("move") {
                val gameId = call.parameters["gameId"]!!
                val monsterId = call.parameters["monsterId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid monster ID")
                val loc = call.receiveOrNull<GridLoc>()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing gridX or gridY")

                call.respond(moveMonster(gameId, monsterId, loc))
                sendUpdate(gameId)
            }
        }
    }
}
