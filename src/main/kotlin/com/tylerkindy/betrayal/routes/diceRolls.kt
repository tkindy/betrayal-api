package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.db.getLatestRoll
import com.tylerkindy.betrayal.db.rollDice
import com.tylerkindy.betrayal.sendUpdate
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable

val diceRollRoutes: Route.() -> Unit = {
    route("dice") {
        get {
            val gameId = call.parameters["gameId"]!!

            val latestRoll = getLatestRoll(gameId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "")
            call.respond(latestRoll)
        }

        post {
            val gameId = call.parameters["gameId"]!!
            val numDice = call.receiveOrNull<RollDiceBody>()?.numDice
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing numDice")

            call.respond(rollDice(gameId, numDice))
            sendUpdate(gameId)
        }
    }
}

@Serializable
data class RollDiceBody(val numDice: Int)
