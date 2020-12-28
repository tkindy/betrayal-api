package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.defs.CardType
import com.tylerkindy.betrayal.defs.events
import com.tylerkindy.betrayal.defs.items
import com.tylerkindy.betrayal.defs.omens
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

fun createCardStacks(gameId: String) {
    val itemStackContents = items.shuffled()
        .mapIndexed { i, item ->
            CardStackContentRequest(
                index = i.toShort(),
                cardDefId = item.id
            )
        }
    val eventStackContents = events.shuffled()
        .mapIndexed { i, event ->
            CardStackContentRequest(
                index = i.toShort(),
                cardDefId = event.id
            )
        }
    val omenStackContents = omens.shuffled()
        .mapIndexed { i, omen ->
            CardStackContentRequest(
                index = i.toShort(),
                cardDefId = omen.id
            )
        }

    transaction {
        createCardStack(gameId, CardType.ITEM, itemStackContents)
        createCardStack(gameId, CardType.EVENT, eventStackContents)
        createCardStack(gameId, CardType.OMEN, omenStackContents)
    }
}

fun createCardStack(gameId: String, cardType: CardType, contents: List<CardStackContentRequest>) {
    val stackId = CardStacks.insert {
        it[this.gameId] = gameId
        it[cardTypeId] = cardType.id
        it[curIndex] = 0
    } get CardStacks.id

    CardStackContents.batchInsert(contents) {
        this[CardStackContents.stackId] = stackId
        this[CardStackContents.index] = it.index
        this[CardStackContents.cardDefId] = it.cardDefId
    }
}

data class CardStackContentRequest(
    val index: Short,
    val cardDefId: Short
)
