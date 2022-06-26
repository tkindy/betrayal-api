package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.*
import com.tylerkindy.betrayal.db.*
import com.tylerkindy.betrayal.routes.LobbyServerMessage.PlayersMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.collections.set

val lobbyRoutes: Routing.() -> Unit = {
    route("lobbies") {
        post {
            val lobbyRequest = call.receiveOrNull<LobbyRequest>()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "'hostName' is required")

            val (hostName) = lobbyRequest;
            validatePlayerName(hostName)

            val lobbyId = buildLobbyId()
            lobbyStates[lobbyId] = LobbyState()
            call.respond(Lobby(id = lobbyId))
        }

        route("{lobbyId}") {
            webSocket {
                val lobbyId = call.parameters["lobbyId"]!!
                val curState = lobbyStates[lobbyId] ?: return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Unknown lobby"
                    )
                )

                val (name) = parseMessage<NameMessage>(incoming.receive())
                    ?: return@webSocket close(
                        CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "Expected name message"
                        )
                    )

                validatePlayerName(name)?.let {
                    return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, it))
                }

                if (name in curState.players.map { it.name }) {
                    return@webSocket close(
                        CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "A player in the lobby is already using the name $name"
                        )
                    )
                }

                val player = PlayerState(name, this)
                val newState = lobbyStates.computeIfPresent(lobbyId) { _, state ->
                    state.copy(
                        players = state.players + player
                    )
                }!!

                val playerNamesMessage =
                    Json.encodeToString(
                        PlayersMessage(players = newState.players.map { it.name }) as LobbyServerMessage
                    )
                newState.players.forEach { player ->
                    player.session.send(playerNamesMessage)
                }

                // Wait for client to close the WebSocket
                incoming.receiveCatching()

                val removedState = lobbyStates.computeIfPresent(lobbyId) { _, state ->
                    state.copy(
                        players = state.players - player
                    )
                }!!

                val newPlayerNamesMessage =
                    Json.encodeToString(
                        PlayersMessage(players = removedState.players.map { it.name }) as LobbyServerMessage
                    )

                removedState.players.forEach { player ->
                    player.session.send(newPlayerNamesMessage)
                }
            }

            post("start-game") {
                val lobbyId = call.parameters["lobbyId"]!!
                val state = lobbyStates[lobbyId] ?: return@post call.respond(HttpStatusCode.NotFound, Unit)
                val gameName = "name"

                transaction {
                    Games.insert {
                        it[id] = lobbyId
                        it[name] = gameName
                    }
                }

                insertStartingRooms(lobbyId)
                insertStartingPlayers(lobbyId, state.players)
                createRoomStack(lobbyId)
                createCardStacks(lobbyId)

                gameUpdateManager.sendUpdate(lobbyId)

                val joinGameMessage = Json.encodeToString(
                    LobbyServerMessage.JoinGame as LobbyServerMessage
                )
                state.players.forEach { player ->
                    player.session.send(joinGameMessage)
                }

                call.respond(Game(id = lobbyId, name = gameName))
            }
        }

    }
}

private val lobbyIdCharacters = ('A'..'Z').toList()
private fun buildLobbyId(): String {
    return (0 until 6)
        .map { lobbyIdCharacters.random() }
        .fold(StringBuilder()) { builder, char -> builder.append(char) }
        .toString()
}

private inline fun <reified T> parseMessage(frame: Frame): T? {
    val textFrame = frame as? Frame.Text
        ?: return null
    return Json.decodeFromString(textFrame.readText())
}

@Serializable
data class NameMessage(val name: String)

@Serializable
sealed class LobbyServerMessage {
    @Serializable
    @SerialName("players")
    data class PlayersMessage(val players: List<String>) : LobbyServerMessage()

    @Serializable
    @SerialName("join")
    object JoinGame : LobbyServerMessage()
}

private fun insertStartingPlayers(gameId: String, players: List<PlayerState>) {
    val characters = getRandomCharacters(players.size)
    transaction {
        players.shuffled()
            .zip(characters.shuffled())
            .forEach { (player, character) ->
                Players.insert {
                    it[this.gameId] = gameId
                    it[name] = player.name
                    it[characterId] = character.id
                    it[gridX] = entranceHallLoc.gridX
                    it[gridY] = entranceHallLoc.gridY
                    it[speedIndex] = character.speed.startingIndex.toShort()
                    it[mightIndex] = character.might.startingIndex.toShort()
                    it[sanityIndex] = character.sanity.startingIndex.toShort()
                    it[knowledgeIndex] = character.knowledge.startingIndex.toShort()
                }
            }
    }
}
