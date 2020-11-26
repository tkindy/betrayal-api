package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.Direction
import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.Room
import com.tylerkindy.betrayal.defs.RoomDefinition
import com.tylerkindy.betrayal.defs.rooms
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

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
    val def: RoomDefinition,
    val loc: GridLoc
)
val entranceHallLoc = GridLoc(4, 3)
private val startingRooms = listOf(
    // Entrance Hall
    StartingRoom(rooms[0]!!, entranceHallLoc),
    // Foyer
    StartingRoom(rooms[1]!!, GridLoc(3, 3)),
    // Grand Staircase
    StartingRoom(rooms[2]!!, GridLoc(2, 3)),
    // Upper Landing
    StartingRoom(rooms[8]!!, GridLoc(-8, -8)),
    // Roof Landing
    StartingRoom(rooms[10]!!, GridLoc(10, -8)),
    // Basement Landing
    StartingRoom(rooms[33]!!, GridLoc(2, 12))
)
fun insertStartingRooms(gameId: String) {
    transaction {
        Rooms.batchInsert(startingRooms) { (def, loc) ->
            this[Rooms.gameId] = gameId
            this[Rooms.roomDefId] = def.id
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
