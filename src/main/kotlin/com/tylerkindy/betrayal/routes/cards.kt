package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.Card
import com.tylerkindy.betrayal.GiveToPlayerBody
import com.tylerkindy.betrayal.db.discardDrawnCard
import com.tylerkindy.betrayal.db.drawEvent
import com.tylerkindy.betrayal.db.drawItem
import com.tylerkindy.betrayal.db.drawOmen
import com.tylerkindy.betrayal.db.getDrawnCard
import com.tylerkindy.betrayal.db.giveDrawnCardToPlayer
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val cardRoutes: Route.() -> Unit = {
    route("cards") {
        route("items") {
            post("draw") {
                val gameId = call.parameters["gameId"]!!
                call.respondWithCard(drawItem(gameId))
            }
        }

        route("events") {
            post("draw") {
                val gameId = call.parameters["gameId"]!!
                call.respondWithCard(drawEvent(gameId))
            }
        }

        route("omens") {
            post("draw") {
                val gameId = call.parameters["gameId"]!!
                call.respondWithCard(drawOmen(gameId))
            }
        }

        route("drawn") {
            get {
                val gameId = call.parameters["gameId"]!!
                val card = getDrawnCard(gameId)

                if (card == null) {
                    call.respond(HttpStatusCode.NotFound, "")
                    return@get
                }

                call.respondWithCard(card)
            }

            post("discard") {
                val gameId = call.parameters["gameId"]!!
                discardDrawnCard(gameId)
                call.respond(HttpStatusCode.OK, "")
            }

            post("giveToPlayer") {
                val gameId = call.parameters["gameId"]!!
                val playerId = call.receiveOrNull<GiveToPlayerBody>()?.playerId
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing playerId")

                call.respond(giveDrawnCardToPlayer(gameId, playerId))
            }
        }
    }
}

// Workaround because of ktor top-level serialization bug
// https://github.com/ktorio/ktor/issues/1783#issuecomment-727645523
suspend inline fun ApplicationCall.respondWithCard(card: Card) {
    response.pipeline.execute(
        this,
        TextContent(
            text = Json.encodeToString(card),
            ContentType.Application.Json,
            HttpStatusCode.OK
        )
    )
}
