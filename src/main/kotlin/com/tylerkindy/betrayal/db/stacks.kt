package com.tylerkindy.betrayal.db

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

enum class StackType(val id: Short) {
    ROOM(0), EVENT(1), ITEM(2), OMEN(3)
}

fun draw(gameId: String, stackType: StackType): Short {
    val stack = Stacks.select { (Stacks.gameId eq gameId) and (Stacks.stackTypeId eq stackType.id) }
        .firstOrNull()
        ?.let {
            Stack(
                id = it[Stacks.id],
                gameId = gameId,
                stackType = stackType,
                curIndex = it[Stacks.curIndex]
            )
        } ?: error("No stack of type $stackType found for game $gameId")

    stack.curIndex ?: throw StackEmptyException()

    val content = StackContents.select {
        (StackContents.stackId eq stack.id) and (StackContents.index eq stack.curIndex)
    }
        .firstOrNull()
        ?.let {
            StackContent(
                id = it[StackContents.id],
                stackId = stack.id,
                index = stack.curIndex,
                contentId = it[StackContents.contentId]
            )
        } ?: error("No content at index ${stack.curIndex} in stack ${stack.id}")

    StackContents.deleteWhere { StackContents.id eq content.id }

    val remainingIndices = StackContents
        .slice(StackContents.index)
        .select { StackContents.stackId eq stack.id }
        .orderBy(StackContents.index, SortOrder.ASC)
        .map { it[StackContents.index] }

    val nextIndex = remainingIndices.firstOrNull { it > content.index }
        ?: remainingIndices.firstOrNull()

    Stacks.update(where = { Stacks.id eq stack.id }) {
        it[curIndex] = nextIndex
    }

    return content.contentId
}

class StackEmptyException : IllegalStateException()

data class Stack(
    val id: Int,
    val gameId: String,
    val stackType: StackType,
    val curIndex: Short?
)

data class StackContent(
    val id: Int,
    val stackId: Int,
    val index: Short,
    val contentId: Short,
)
