package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.FlippedRoom
import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.PlaceRoomResponse
import com.tylerkindy.betrayal.RoomStackResponse
import com.tylerkindy.betrayal.StackRoom
import com.tylerkindy.betrayal.defs.RoomDefinition
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
            it[flipped] = false
        } get RoomStacks.id

        RoomStackContents.batchInsert(roomStackContents) {
            this[RoomStackContents.stackId] = stackId
            this[RoomStackContents.index] = it.index
            this[RoomStackContents.roomDefId] = it.roomDefId
        }
    }
}

fun getRoomStackState(gameId: String): RoomStackResponse {
    return transaction {
        val stack = getRoomStack(gameId)
        stack.curIndex ?: return@transaction RoomStackResponse()

        val roomStackContentRow = getRoomInStack(stack.id, stack.curIndex)
            ?: error("No room in stack for game $gameId with index ${stack.curIndex}")

        val roomDefId = roomStackContentRow[RoomStackContents.roomDefId]
        val room = rooms[roomDefId]
            ?: error("No room def with ID $roomDefId")

        if (stack.flipped) {
            RoomStackResponse(
                flippedRoom = FlippedRoom(
                    name = room.name,
                    doorDirections = room.doors,
                    features = room.features
                )
            )
        } else {
            RoomStackResponse(
                nextRoom = StackRoom(
                    possibleFloors = room.floors
                )
            )
        }
    }
}

fun advanceRoomStack(gameId: String): StackRoom {
    return transaction {
        val stack = getRoomStack(gameId)
        stack.curIndex ?: throw StackEmptyException()
        if (stack.flipped) throw AlreadyFlippedException()

        val nextIndex = getNextIndex(stack)
            ?: error("No remaining contents in stack $stack")

        RoomStacks.update(where = { RoomStacks.id eq stack.id }) {
            it[curIndex] = nextIndex
        }
        getRoomInStack(stack.id, nextIndex)
            ?.toStackRoom()
            ?: error("No room in stack for index $nextIndex")
    }
}

fun flipRoom(gameId: String): FlippedRoom {
    return transaction {
        val stack = getRoomStack(gameId)
        stack.curIndex ?: throw StackEmptyException()
        if (stack.flipped) throw AlreadyFlippedException()

        val rotation: Short = 0

        RoomStacks.update(where = { RoomStacks.id eq stack.id }) {
            it[flipped] = true
            it[this.rotation] = rotation
        }

        getRoomInStack(stack.id, stack.curIndex)
            ?.toFlippedRoom(rotation)
            ?: error("No room at index ${stack.curIndex} in room stack ${stack.id}")
    }
}

fun rotateFlipped(gameId: String): FlippedRoom {
    return transaction {
        val stack = getRoomStack(gameId)
        stack.curIndex ?: throw StackEmptyException()
        if (!stack.flipped) throw NotFlippedException()

        val newRotation = rotate(
            stack.rotation ?: error("Missing rotation value for flipped room stack")
        )

        RoomStacks.update(where = { RoomStacks.id eq stack.id }) {
            it[rotation] = newRotation
        }

        getRoomInStack(stack.id, stack.curIndex)
            ?.toFlippedRoom(newRotation)
            ?: error("No room at index ${stack.curIndex} in room stack ${stack.id}")
    }
}

fun placeRoom(gameId: String, loc: GridLoc): PlaceRoomResponse {
    return transaction {
        val stack = getRoomStack(gameId)
        stack.curIndex ?: throw StackEmptyException()
        if (!stack.flipped) throw NotFlippedException()
        stack.rotation ?: error("Missing rotation value for flipped room stack")

        // TODO: add backend validation of room placement; currently relying on the frontend to do it
        val contentsRow = getRoomInStack(stack.id, stack.curIndex)
            ?: error("No room at index ${stack.curIndex} in room stack ${stack.id}")

        val rowId = contentsRow[RoomStackContents.id]
        val room = contentsRow.toRoomDef()

        Rooms.insert {
            it[this.gameId] = gameId
            it[roomDefId] = room.id
            it[gridX] = loc.gridX
            it[gridY] = loc.gridY
            it[rotation] = stack.rotation
        }

        RoomStackContents.deleteWhere { RoomStackContents.id eq rowId }

        val nextIndex = getNextIndex(stack)
        RoomStacks.update(where = { RoomStacks.id eq stack.id }) {
            it[curIndex] = nextIndex
            it[flipped] = false
            it[rotation] = null
        }

        return@transaction PlaceRoomResponse(
            rooms = getRooms(gameId),
            nextRoom = nextIndex?.let { getRoomInStack(stack.id, it) }?.toStackRoom()
        )
    }
}

private fun getRoomStack(gameId: String): RoomStack {
    return RoomStacks.select { RoomStacks.gameId eq gameId }
        .firstOrNull()
        ?.toRoomStack() ?: error("No room stack found for game $gameId")
}

private fun getNextIndex(stack: RoomStack): Short? {
    stack.curIndex ?: throw StackEmptyException()

    val remainingIndices = RoomStackContents
        .slice(RoomStackContents.index)
        .select { RoomStackContents.stackId eq stack.id }
        .orderBy(RoomStackContents.index, SortOrder.ASC)
        .map { it[RoomStackContents.index] }

    return remainingIndices.firstOrNull { it > stack.curIndex }
        ?: remainingIndices.firstOrNull()
}

private fun rotate(rotation: Short): Short {
    return ((rotation + 1) % 4).toShort()
}

class StackEmptyException : IllegalStateException()
class AlreadyFlippedException : IllegalStateException()
class NotFlippedException : IllegalStateException()

data class RoomStack(
    val id: Int,
    val gameId: String,
    val curIndex: Short?,
    val flipped: Boolean,
    val rotation: Short?
)

data class RoomStackContentRequest(
    val index: Short,
    val roomDefId: Short
)

fun getRoomInStack(stackId: Int, index: Short): ResultRow? {
    return RoomStackContents.select {
        (RoomStackContents.stackId eq stackId) and (RoomStackContents.index eq index)
    }
        .firstOrNull()
}

fun ResultRow.toRoomStack(): RoomStack {
    return RoomStack(
        id = this[RoomStacks.id],
        gameId = this[RoomStacks.gameId],
        curIndex = this[RoomStacks.curIndex],
        flipped = this[RoomStacks.flipped],
        rotation = this[RoomStacks.rotation]
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

fun ResultRow.toRoomDef(): RoomDefinition {
    val roomDefId = this[RoomStackContents.roomDefId]
    return rooms[roomDefId]
        ?: error("No room def with ID $roomDefId")
}

fun ResultRow.toFlippedRoom(rotation: Short): FlippedRoom {
    val room = toRoomDef()
    return FlippedRoom(
        name = room.name,
        doorDirections = rotateDoors(room.doors, rotation),
        features = room.features
    )
}
