package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.Direction
import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.Room
import com.tylerkindy.betrayal.defs.rooms
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun ResultRow.toRoom(): Room {
    val roomDefId = this[Rooms.roomDefId]
    val roomDef = rooms[roomDefId]
        ?: error("No room defined with ID $roomDefId")

    return Room(
        id = this[Rooms.id],
        name = roomDef.name,
        floors = roomDef.floors,
        doorDirections = rotateDoors(roomDef.doors, this[Rooms.rotation]),
        features = roomDef.features,
        loc = GridLoc(gridX = this[Rooms.gridX], gridY = this[Rooms.gridY]),
        description = roomDef.description,
        barrier = roomDef.barrier
    )
}

fun getRooms(gameId: String): List<Room> {
    return transaction {
        Rooms.select { Rooms.gameId eq gameId }
            .map { it.toRoom() }
    }
}

private data class StartingRoom(
    val roomDefId: Short,
    val loc: GridLoc
)
val entranceHallLoc = GridLoc(4, 3)
private val startingRooms = listOf(
    // Entrance Hall
    StartingRoom(0, entranceHallLoc),
    // Foyer
    StartingRoom(1, GridLoc(3, 3)),
    // Grand Staircase
    StartingRoom(2, GridLoc(2, 3)),
    // Upper Landing
    StartingRoom(8, GridLoc(-8, -8)),
    // Roof Landing
    StartingRoom(10, GridLoc(10, -8)),
    // Basement Landing
    StartingRoom(33, GridLoc(2, 12))
)
val startingRoomIds = startingRooms.map { it.roomDefId }

fun insertStartingRooms(gameId: String) {
    transaction {
        Rooms.batchInsert(startingRooms) { (roomDefId, loc) ->
            this[Rooms.gameId] = gameId
            this[Rooms.roomDefId] = roomDefId
            this[Rooms.gridX] = loc.gridX
            this[Rooms.gridY] = loc.gridY
            this[Rooms.rotation] = 0
        }
    }
}

private val rotated = mapOf(
    Direction.EAST to Direction.NORTH,
    Direction.NORTH to Direction.WEST,
    Direction.WEST to Direction.SOUTH,
    Direction.SOUTH to Direction.EAST
)
internal fun rotateDoors(directions: Set<Direction>, rotation: Short): Set<Direction> {
    if (!(0..3).contains(rotation)) {
        throw IllegalArgumentException("Invalid rotation $rotation")
    }
    if (directions.size == Direction.values().size) {
        return directions
    }

    return directions.map { direction ->
        (0 until rotation).fold(direction) { dir, _ -> rotated[dir]!! }
    }.toSet()
}

fun getRoomAtLoc(gameId: String, loc: GridLoc): Room? {
    return transaction {
        Rooms.select {
            (Rooms.gameId eq gameId) and
                (Rooms.gridX eq loc.gridX) and
                (Rooms.gridY eq loc.gridY)
        }.firstOrNull()?.toRoom()
    }
}

fun moveRoom(gameId: String, roomId: Int, loc: GridLoc) {
    transaction {
        assertRoomInGame(gameId, roomId)
        if (getRoomAtLoc(gameId, loc) != null) {
            throw IllegalArgumentException("Can't place room in occupied spot")
        }

        val originalLoc = Rooms.slice(Rooms.gridX, Rooms.gridY)
            .select { Rooms.id eq roomId }
            .first()
            .let {
                GridLoc(
                    gridX = it[Rooms.gridX],
                    gridY = it[Rooms.gridY]
                )
            }

        Rooms.update(where = { Rooms.id eq roomId }) {
            it[gridX] = loc.gridX
            it[gridY] = loc.gridY
        }
        Players.update(
            where = {
                (Players.gameId eq gameId) and
                    (Players.gridX eq originalLoc.gridX) and
                    (Players.gridY eq originalLoc.gridY)
            }
        ) {
            it[gridX] = loc.gridX
            it[gridY] = loc.gridY
        }
        Monsters.update(where = {
            (Monsters.gameId eq gameId) and
                    (Monsters.gridX eq originalLoc.gridX) and
                    (Monsters.gridY eq originalLoc.gridY)
        }) {
            it[gridX] = loc.gridX
            it[gridY] = loc.gridY
        }
    }
}

fun assertRoomInGame(gameId: String, roomId: Int) {
    Rooms.select { (Rooms.id eq roomId) and (Rooms.gameId eq gameId) }
        .firstOrNull()
        ?: throw IllegalArgumentException("Room $roomId not in game $gameId")
}
