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

fun createRoomStack(gameId: String) {
    val roomStackContents = initialStackRooms.shuffled()
        .mapIndexed { i, room ->
            RoomStackContentRequest(
                index = i.toShort(),
                roomDefId = room.id
            )
        }

    transaction {
        val stackId = RoomStacks.insert {
            it[this.gameId] = gameId
            it[curIndex] = 0
        } get RoomStacks.id

        RoomStackContents.batchInsert(roomStackContents) {
            this[RoomStackContents.stackId] = stackId
            this[RoomStackContents.index] = it.index
            this[RoomStackContents.roomDefId] = it.roomDefId
        }
    }
}

fun getStackRoom(gameId: String): StackRoom? {
    return transaction {
        val stack = RoomStacks.select { RoomStacks.gameId eq gameId }
            .firstOrNull()
            ?.toRoomStack() ?: error("No room stack found for game $gameId")

        stack.curIndex ?: return@transaction null

        RoomStackContents.select {
            (RoomStackContents.stackId eq stack.id) and (RoomStackContents.index eq stack.curIndex)
        }
            .firstOrNull()
            ?.toStackRoom()
    }
}

fun advanceRoomStack(gameId: String): StackRoom {
    return transaction {
        val stack = getRoomStack(gameId)
        stack.curIndex ?: throw StackEmptyException()

        val nextIndex = getNextIndex(stack)
        RoomStacks.update(where = { RoomStacks.id eq stack.id }) {
            it[curIndex] = nextIndex
        }
        RoomStackContents.select {
            (RoomStackContents.stackId eq stack.id) and (RoomStackContents.index eq nextIndex)
        }
            .firstOrNull()
            ?.toStackRoom()
            ?: error("No room in stack for index $nextIndex")
    }
}

fun flipRoom(gameId: String): Short {
    val content = transaction {
        val stack = getRoomStack(gameId)
        stack.curIndex ?: throw StackEmptyException()

        val content = RoomStackContents.select {
            (RoomStackContents.stackId eq stack.id) and (RoomStackContents.index eq stack.curIndex)
        }
            .firstOrNull()
            ?.let {
                RoomStackContent(
                    id = it[RoomStackContents.id],
                    stackId = stack.id,
                    index = stack.curIndex,
                    roomDefId = it[RoomStackContents.roomDefId]
                )
            } ?: error("No room at index ${stack.curIndex} in room stack ${stack.id}")

        RoomStackContents.deleteWhere { RoomStackContents.id eq content.id }

        val nextIndex = getNextIndex(stack)
        RoomStacks.update(where = { RoomStacks.id eq stack.id }) {
            it[curIndex] = nextIndex
        }

        content
    }

    return content.roomDefId
}

private fun getRoomStack(gameId: String): RoomStack {
    return RoomStacks.select { RoomStacks.gameId eq gameId }
        .firstOrNull()
        ?.toRoomStack() ?: error("No room stack found for game $gameId")
}

private fun getNextIndex(stack: RoomStack): Short {
    stack.curIndex ?: throw StackEmptyException()

    val remainingIndices = RoomStackContents
        .slice(RoomStackContents.index)
        .select { RoomStackContents.stackId eq stack.id }
        .orderBy(RoomStackContents.index, SortOrder.ASC)
        .map { it[RoomStackContents.index] }

    return remainingIndices.firstOrNull { it > stack.curIndex }
        ?: remainingIndices.firstOrNull()
        ?: error("No remaining contents in stack $stack")
}

class StackEmptyException : IllegalStateException()

data class RoomStack(
    val id: Int,
    val gameId: String,
    val curIndex: Short?,
    val flipped: Boolean
)

data class RoomStackContentRequest(
    val index: Short,
    val roomDefId: Short
)

data class RoomStackContent(
    val id: Int,
    val stackId: Int,
    val index: Short,
    val roomDefId: Short,
)

fun ResultRow.toRoomStack(): RoomStack {
    return RoomStack(
        id = this[RoomStacks.id],
        gameId = this[RoomStacks.gameId],
        curIndex = this[RoomStacks.curIndex],
        flipped = this[RoomStacks.flipped]
    )
}

fun ResultRow.toStackRoom(): StackRoom {
    val roomDefId = this[RoomStackContents.roomDefId]
    val room = rooms[roomDefId]
        ?: error("No room def with ID $roomDefId")
    return StackRoom(
        possibleFloors = room.floors
    )
}
