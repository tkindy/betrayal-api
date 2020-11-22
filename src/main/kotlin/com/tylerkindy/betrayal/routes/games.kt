package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.Game
import com.tylerkindy.betrayal.GameRequest
import com.tylerkindy.betrayal.Player
import com.tylerkindy.betrayal.db.Games
import com.tylerkindy.betrayal.db.Players
import com.tylerkindy.betrayal.defs.CharacterDefinition
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
            val game = transaction {
                val gameRow = Games.select { Games.id eq id }
                    .firstOrNull() ?: return@transaction null

                val players =
                    Players.select { Players.gameId eq id }
                        .map {
                            val characterId = it[Players.characterId]
                            val characterDef = characters[characterId]
                                ?: error("No character defined with ID $characterId")

                            Player(
                                id = it[Players.id],
                                characterName = characterDef.name,
                                color = characterDef.color,
                                speed = characterDef.speed.toTrait(it[Players.speedIndex].toInt()),
                                might = characterDef.might.toTrait(it[Players.mightIndex].toInt()),
                                sanity = characterDef.sanity.toTrait(it[Players.sanityIndex].toInt()),
                                knowledge = characterDef.knowledge.toTrait(it[Players.knowledgeIndex].toInt())
                            )
                        }

                Game(id = gameRow[Games.id], name = gameRow[Games.name], players = players)
            } ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(game)
        }

        post {
            val gameRequest = call.receiveOrNull<GameRequest>()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "'name' and 'numPlayers' are required")

            if (gameRequest.name.length > 32) {
                return@post call.respond(HttpStatusCode.BadRequest, "Name can only be up to 32 characters")
            }
            if (!(3..6).contains(gameRequest.numPlayers)) {
                return@post call.respond(HttpStatusCode.BadRequest, "Must have between 3 and 6 players")
            }

            val gameId = buildGameId()
            val characters = getRandomCharacters(gameRequest.numPlayers)

            val players = transaction {
                Games.insert {
                    it[id] = gameId
                    it[name] = gameRequest.name
                }

                characters.map { character ->
                    val playerId = Players.insert {
                        it[this.gameId] = gameId
                        it[characterId] = character.id
                        it[speedIndex] = character.speed.startingIndex.toShort()
                        it[mightIndex] = character.might.startingIndex.toShort()
                        it[sanityIndex] = character.might.startingIndex.toShort()
                        it[knowledgeIndex] = character.knowledge.startingIndex.toShort()
                    } get Players.id

                    Player(
                        id = playerId,
                        characterName = character.name,
                        color = character.color,
                        speed = character.speed.toTrait(),
                        might = character.might.toTrait(),
                        sanity = character.sanity.toTrait(),
                        knowledge = character.knowledge.toTrait()
                    )
                }
            }

            call.respond(Game(id = gameId, name = gameRequest.name, players = players))
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

private fun getRandomCharacters(numPlayers: Int): List<CharacterDefinition> {
    return characters.values
        .groupBy { it.color }.values
        .map { it.random() } // randomly pick one of each color
        .shuffled()
        .subList(0, numPlayers)
}
