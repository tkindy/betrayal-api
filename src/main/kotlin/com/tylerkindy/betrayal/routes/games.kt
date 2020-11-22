package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.Game
import com.tylerkindy.betrayal.GameRequest
import com.tylerkindy.betrayal.Games
import com.tylerkindy.betrayal.Player
import com.tylerkindy.betrayal.Players
import com.tylerkindy.betrayal.Trait
import com.tylerkindy.betrayal.defs.characters
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

val gameRoutes: Routing.() -> Unit = {
    route("games") {
        get("{id}") {
            val id = call.parameters["id"]!!
            val gameRow = transaction {
                Games.select { Games.id eq id }
                    .firstOrNull()
            } ?: return@get call.respond(HttpStatusCode.NotFound)

            val players = transaction {
                Players.select { Players.gameId eq id }
            }
                .map {
                    val characterId = it[Players.characterId]
                    val characterDef = characters[characterId]
                        ?: error("No character defined with ID $characterId")

                    Player(
                        id = it[Players.id],
                        characterName = characterDef.name,
                        color = characterDef.color,
                        speed = buildTrait(it[Players.speedIndex], characterDef.speedScale),
                        might = buildTrait(it[Players.mightIndex], characterDef.mightScale),
                        sanity = buildTrait(it[Players.sanityIndex], characterDef.sanityScale),
                        knowledge = buildTrait(it[Players.knowledgeIndex], characterDef.knowledgeScale)
                    )
                }

            call.respond(
                Game(id = gameRow[Games.id], name = gameRow[Games.name], players = players)
            )
        }

        post {
            val gameRequest = call.receiveOrNull<GameRequest>()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing 'name'")

            if (gameRequest.name.length > 32) {
                return@post call.respond(HttpStatusCode.BadRequest, "Name can only be up to 32 characters")
            }

            val gameId = buildGameId()
            transaction {
                Games.insert {
                    it[id] = gameId
                    it[name] = gameRequest.name
                }
            }

            call.respond(Game(id = gameId, name = gameRequest.name, players = emptyList()))
        }
    }
}

private val gameIdCharacters = ('A'..'Z').toList()
private fun buildGameId(): String {
    return (0 until 6)
        .map { gameIdCharacters.random() }
        .fold(StringBuilder()) { builder, char -> builder.append(char) }
        .toString()
}

private fun buildTrait(index: Short, scale: List<Int>): Trait =
    Trait(value = scale[index.toInt()], index = index)
