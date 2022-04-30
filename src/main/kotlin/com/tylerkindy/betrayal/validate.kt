package com.tylerkindy.betrayal

import com.tylerkindy.betrayal.db.LobbyPlayers
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun validatePlayerName(name: String): String? {
    return if (name.length !in 1..20) {
        "Name must be between 1 and 20 characters"
    } else null
}

fun validatePlayerName(name: String, lobbyId: String): String? =
    validatePlayerName(name)
        ?: transaction {
            LobbyPlayers.select {
                (LobbyPlayers.lobbyId eq lobbyId) and
                        (LobbyPlayers.name eq name)
            }
                .firstOrNull()
                ?.let {
                    "There's already a player in the lobby with that name"
                }
        }
