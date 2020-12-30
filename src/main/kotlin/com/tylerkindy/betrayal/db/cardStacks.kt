package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.EventCard
import com.tylerkindy.betrayal.ItemCard
import com.tylerkindy.betrayal.OmenCard
import com.tylerkindy.betrayal.defs.CardType
import com.tylerkindy.betrayal.defs.events
import com.tylerkindy.betrayal.defs.items
import com.tylerkindy.betrayal.defs.omens
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun createCardStacks(gameId: String) {
    val itemStackContents = items.values.shuffled()
        .mapIndexed { i, item ->
            CardStackContentRequest(
                index = i.toShort(),
                cardDefId = item.id
            )
        }
    val eventStackContents = events.values.shuffled()
        .mapIndexed { i, event ->
            CardStackContentRequest(
                index = i.toShort(),
                cardDefId = event.id
            )
        }
    val omenStackContents = omens.values.shuffled()
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

private fun createCardStack(gameId: String, cardType: CardType, contents: List<CardStackContentRequest>) {
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

fun drawItem(gameId: String): ItemCard {
    return drawCard(gameId, CardType.ITEM) {
        val cardDefId = this[CardStackContents.cardDefId]
        val cardDef = items[cardDefId] ?: error("No item def with ID $cardDefId")
        ItemCard(
            id = this[CardStackContents.id],
            name = cardDef.name,
            subtype = cardDef.subtype,
            flavorText = cardDef.flavorText,
            description = cardDef.description,
            rollTable = cardDef.rollTable
        )
    }
}

fun drawEvent(gameId: String): EventCard {
    return drawCard(gameId, CardType.EVENT) {
        val cardDefId = this[CardStackContents.cardDefId]
        val cardDef = events[cardDefId] ?: error("No event def with ID $cardDefId")
        EventCard(
            id = this[CardStackContents.id],
            name = cardDef.name,
            condition = cardDef.condition,
            flavorText = cardDef.flavorText,
            description = cardDef.description,
            rollTable = cardDef.rollTable
        )
    }
}

fun drawOmen(gameId: String): OmenCard {
    return drawCard(gameId, CardType.OMEN) {
        val cardDefId = this[CardStackContents.cardDefId]
        val cardDef = omens[cardDefId] ?: error("No omen def with ID $cardDefId")
        OmenCard(
            id = this[CardStackContents.id],
            name = cardDef.name,
            subtype = cardDef.subtype,
            flavorText = cardDef.flavorText,
            description = cardDef.description,
            rollTable = cardDef.rollTable
        )
    }
}

private fun <T> drawCard(
    gameId: String,
    cardType: CardType,
    buildCard: ResultRow.() -> T
): T {
    return transaction {
        val drawnCard = DrawnCards.select { DrawnCards.gameId eq gameId }
            .firstOrNull()

        if (drawnCard != null) {
            throw IllegalStateException("There's already a card drawn")
        }

        val stack = CardStacks.select {
            (CardStacks.gameId eq gameId) and (CardStacks.cardTypeId eq cardType.id)
        }
            .firstOrNull()
            ?.toCardStack() ?: error("No $cardType stack found for game $gameId")

        stack.curIndex ?: throw IllegalStateException()

        val contentRow = CardStackContents.select {
            (CardStackContents.stackId eq stack.id) and (CardStackContents.index eq stack.curIndex)
        }
            .firstOrNull()
            ?: error("No $cardType card in stack ${stack.id} at index ${stack.curIndex}")

        CardStackContents.deleteWhere { CardStackContents.id eq contentRow[CardStackContents.id] }
        DrawnCards.insert {
            it[this.gameId] = gameId
            it[cardTypeId] = cardType.id
            it[cardDefId] = contentRow[CardStackContents.cardDefId]
        }

        val nextIndex = CardStackContents.slice(CardStackContents.index)
            .select { CardStackContents.stackId eq stack.id }
            .orderBy(CardStackContents.index, SortOrder.ASC)
            .firstOrNull()
            ?.get(CardStackContents.index)

        CardStacks.update(where = { CardStacks.id eq stack.id }) {
            it[curIndex] = nextIndex
        }

        contentRow.buildCard()
    }
}

private fun ResultRow.toCardStack(): CardStack {
    return CardStack(
        id = this[CardStacks.id],
        gameId = this[CardStacks.gameId],
        cardType = CardType.fromId(this[CardStacks.cardTypeId]),
        curIndex = this[CardStacks.curIndex]
    )
}

data class CardStack(
    val id: Int,
    val gameId: String,
    val cardType: CardType,
    val curIndex: Short?
)

data class CardStackContentRequest(
    val index: Short,
    val cardDefId: Short
)
