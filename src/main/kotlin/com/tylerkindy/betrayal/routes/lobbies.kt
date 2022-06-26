package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.*
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

                val playerNamesFrame =
                    Frame.Text(
                        Json.encodeToString(
                            PlayersMessage(players = newState.players.map { it.name }) as LobbyServerMessage
                        )
                    )
                newState.players.forEach { player ->
                    player.session.send(playerNamesFrame)
                }

                // Wait for client to close the WebSocket
                incoming.receiveCatching()

                val removedState = lobbyStates.computeIfPresent(lobbyId) { _, state ->
                    state.copy(
                        players = state.players - player
                    )
                }!!

                val newPlayerNamesFrame = Frame.Text(Json.encodeToString(
                    removedState.players.map { it.name }
                ))
                removedState.players.forEach { player ->
                    player.session.send(newPlayerNamesFrame)
                }
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

private val playerPasswordCharacters =
    ('A'..'Z').toList() +
            ('a'..'z').toList() +
            ('0'..'9').toList()

private fun buildPlayerPassword(): String {
    return (0 until 8)
        .map { playerPasswordCharacters.random() }
        .fold(StringBuilder(), StringBuilder::append)
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
}
