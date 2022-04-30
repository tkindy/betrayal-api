package com.tylerkindy.betrayal.routes

import com.tylerkindy.betrayal.LobbyRequest
import com.tylerkindy.betrayal.addUpdateRoute
import com.tylerkindy.betrayal.db.Lobbies
import com.tylerkindy.betrayal.db.LobbyPlayers
import com.tylerkindy.betrayal.lobbyUpdateManager
import com.tylerkindy.betrayal.validatePlayerName
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
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

            call.respond(HttpStatusCode.NoContent, "")
        }

        route("{lobbyId}") {
            addUpdateRoute(lobbyUpdateManager, "lobbyId")
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
