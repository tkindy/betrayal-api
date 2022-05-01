package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.*
import com.tylerkindy.betrayal.db.Lobbies
import com.tylerkindy.betrayal.db.LobbyPlayers
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

val lobbyRoutes: Routing.() -> Unit = {
    route("lobbies") {
        post {
            val lobbyRequest = call.receiveOrNull<LobbyRequest>()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "'hostName' is required")

            val (hostName) = lobbyRequest;
            validatePlayerName(hostName)

            val lobbyId = buildLobbyId()
            val hostPassword = buildPlayerPassword()

            transaction {
                val hostId = LobbyPlayers.insert {
                    it[this.lobbyId] = lobbyId
                    it[name] = hostName
                    it[password] = hostPassword
                } get LobbyPlayers.id

                Lobbies.insert {
                    it[id] = lobbyId
                    it[this.hostId] = hostId
                }
            }

            lobbyUpdateManager.sendUpdate(lobbyId)

            call.sessions.set(UserSession(name = hostName, password = hostPassword))
            call.respond(Lobby(id = lobbyId))
        }

        route("{lobbyId}") {
            post("join") {
                val lobbyId = call.parameters["lobbyId"]!!
                transaction {
                    Lobbies.select { Lobbies.id eq lobbyId }
                        .firstOrNull()
                } ?: return@post call.respond(HttpStatusCode.NotFound, "")

                val joinRequest = call.receiveOrNull<JoinLobbyRequest>()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "'name' is required")

                val (name) = joinRequest
                validatePlayerName(name, lobbyId)

                val password = buildPlayerPassword()

                transaction {
                    LobbyPlayers.insert {
                        it[this.lobbyId] = lobbyId
                        it[this.name] = name
                        it[this.password] = password
                    }
                }

                call.sessions.set(UserSession(name = name, password = password))
                call.respond(HttpStatusCode.NoContent, "")
            }

            webSocket {
                val lobbyId = call.parameters["lobbyId"]!!
                transaction {
                    Lobbies.select { Lobbies.id eq lobbyId }
                        .firstOrNull()
                } ?: return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Unknown lobby"
                    )
                )

                val (name, password) = call.sessions.get<UserSession>()
                    ?: return@webSocket close(
                        CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "Missing auth"
                        )
                    )

                transaction {
                    LobbyPlayers.select {
                        (LobbyPlayers.lobbyId eq lobbyId) and
                                (LobbyPlayers.name eq name) and
                                (LobbyPlayers.password eq password)
                    }
                        .firstOrNull()
                } ?: return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Invalid auth"
                    )
                )

                sendUpdates(lobbyUpdateManager, lobbyId)
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
