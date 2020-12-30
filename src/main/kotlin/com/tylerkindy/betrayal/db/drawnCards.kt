package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.Card
import com.tylerkindy.betrayal.defs.CardType
import com.tylerkindy.betrayal.defs.events
import com.tylerkindy.betrayal.defs.items
import com.tylerkindy.betrayal.defs.omens
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun getDrawnCard(gameId: String): Card? {
    return transaction {
        DrawnCards.select { DrawnCards.gameId eq gameId }
            .firstOrNull()
            ?.let { row ->
                when (CardType.fromId(row[DrawnCards.cardTypeId])) {
                    CardType.EVENT -> events[row[DrawnCards.cardDefId]]!!.toCard()
                    CardType.ITEM -> items[row[DrawnCards.cardDefId]]!!.toCard()
                    CardType.OMEN -> omens[row[DrawnCards.cardDefId]]!!.toCard()
                }
            }
    }
}

fun discardDrawnCard(gameId: String) {
    transaction {
        DrawnCards.deleteWhere { DrawnCards.gameId eq gameId }
    }
}
