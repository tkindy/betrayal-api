package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.StackRoom
import com.tylerkindy.betrayal.defs.initialStackRooms
import com.tylerkindy.betrayal.defs.rooms
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

@Suppress("unused")
enum class StackType(val id: Short) {
    ROOM(0), EVENT(1), ITEM(2), OMEN(3);

    companion object {
        private val index = values().associateBy { it.id }
        fun fromId(id: Short) = index[id]
            ?: throw IllegalArgumentException("No stack type with ID $id")
    }
}

fun createStacks(gameId: String) {
    // TODO: items, events, omens
    val roomStackContents = initialStackRooms.shuffled()
        .mapIndexed { i, room ->
            StackContentRequest(
                index = i.toShort(),
                contentId = room.id
            )
        }

    transaction {
        val stackId = Stacks.insert {
            it[this.gameId] = gameId
            it[stackTypeId] = StackType.ROOM.id
            it[curIndex] = 0
        } get Stacks.id

        StackContents.batchInsert(roomStackContents) {
            this[StackContents.stackId] = stackId
            this[StackContents.index] = it.index
            this[StackContents.contentId] = it.contentId
        }
    }
}

fun getStackRoom(gameId: String): StackRoom? {
    return transaction {
        val stack = Stacks.select {
            (Stacks.gameId eq gameId) and (Stacks.stackTypeId eq StackType.ROOM.id)
        }
            .firstOrNull()
            ?.toStack() ?: error("No room stack found for game $gameId")

        stack.curIndex ?: return@transaction null

        StackContents.select {
            (StackContents.stackId eq stack.id) and (StackContents.index eq stack.curIndex)
        }
            .firstOrNull()
            ?.let {
                val roomDefId = it[StackContents.contentId]
                val room = rooms[roomDefId]
                    ?: error("No room def with ID $roomDefId")
                StackRoom(
                    possibleFloors = room.floors
                )
            }
    }
}

fun draw(gameId: String, stackType: StackType): Short {
    val content = transaction {
        val stack = Stacks.select { (Stacks.gameId eq gameId) and (Stacks.stackTypeId eq stackType.id) }
            .firstOrNull()
            ?.toStack() ?: error("No stack of type $stackType found for game $gameId")

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

        content
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

data class StackContentRequest(
    val index: Short,
    val contentId: Short
)

data class StackContent(
    val id: Int,
    val stackId: Int,
    val index: Short,
    val contentId: Short,
)

fun ResultRow.toStack(): Stack {
    return Stack(
        id = this[Stacks.id],
        gameId = this[Stacks.gameId],
        stackType = StackType.fromId(this[Stacks.stackTypeId]),
        curIndex = this[Stacks.curIndex]
    )
}
