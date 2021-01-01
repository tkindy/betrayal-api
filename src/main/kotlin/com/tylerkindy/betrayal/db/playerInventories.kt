package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.HeldCard
import com.tylerkindy.betrayal.defs.CardType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun getPlayerInventory(gameId: String, playerId: Int): List<HeldCard> {
    return transaction {
        assertPlayerInGame(gameId, playerId)

        PlayerInventories.select { PlayerInventories.playerId eq playerId }
            .map {
                HeldCard(
                    id = it[PlayerInventories.id],
                    card = buildCard(
                        CardType.fromId(
                            it[PlayerInventories.cardTypeId]
                        ),
                        it[PlayerInventories.cardDefId]
                    )
                )
            }
    }
}

fun assertPlayerInGame(gameId: String, playerId: Int) {
    Players.select { (Players.id eq playerId) and (Players.gameId eq gameId) }
        .firstOrNull() ?: throw IllegalArgumentException("Player $playerId is not part of game $gameId")
}