package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.LobbyPlayer
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun getHostId(lobbyId: String): Int {
    return transaction {
        Lobbies.select { Lobbies.id eq lobbyId }
            .firstOrNull()
            ?.let {
                it[Lobbies.hostId]
            }
            ?: throw IllegalArgumentException("No lobby found with ID $lobbyId")
    }
}

fun getLobbyPlayers(lobbyId: String): List<LobbyPlayer> {
    return transaction {
        LobbyPlayers.select { LobbyPlayers.lobbyId eq lobbyId }
            .map {
                LobbyPlayer(
                    id = it[LobbyPlayers.id],
                    name = it[LobbyPlayers.name]
                )
            }
    }
}
