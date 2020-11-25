package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.Direction
import com.tylerkindy.betrayal.Room
import com.tylerkindy.betrayal.defs.rooms
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun getRooms(gameId: String): List<Room> {
    return transaction {
        Rooms.select { Rooms.gameId eq gameId }
            .map {
                val roomDefId = it[Rooms.roomDefId]
                val roomDef = rooms[roomDefId]
                    ?: error("No room defined with ID $roomDefId")

                Room(
                    id = it[Rooms.id],
                    name = roomDef.name,
                    floors = roomDef.floors,
                    doors = rotateDoors(roomDef.doors, it[Rooms.rotation]),
                    features = roomDef.features,
                    description = roomDef.description,
                    barrier = roomDef.barrier
                )
            }
    }
}

private val rotated = mapOf(
    Direction.EAST to Direction.NORTH,
    Direction.NORTH to Direction.WEST,
    Direction.WEST to Direction.SOUTH,
    Direction.SOUTH to Direction.EAST
)
fun rotateDoors(directions: Set<Direction>, rotation: Short): Set<Direction> {
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
