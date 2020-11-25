package com.tylerkindy.betrayal.defs

import com.tylerkindy.betrayal.Barrier
import com.tylerkindy.betrayal.BarrierDirection
import com.tylerkindy.betrayal.Direction
import com.tylerkindy.betrayal.Feature
import com.tylerkindy.betrayal.Floor
import org.apache.commons.csv.CSVFormat
import java.io.InputStreamReader

data class RoomDefinition(
    val id: Short,
    val name: String,
    val floors: Set<Floor>,
    val doors: Set<Direction>,
    val features: List<Feature>,
    val description: String?,
    val barrier: Barrier?
)

private val fileStream = RoomDefinition::class.java.getResourceAsStream("/rooms.csv")
val rooms = CSVFormat.DEFAULT.withNullString("").withFirstRecordAsHeader()
    .parse(InputStreamReader(fileStream))
    .map {
        RoomDefinition(
            id = it["id"].toShort(),
            name = it["name"],
            floors = it["floors"].map(Floor.Companion::parse).toSet(),
            doors = it["doors"].map(Direction.Companion::parse).toSet(),
            features = it["features"].map(Feature.Companion::parse),
            description = it["description"],
            barrier = parseBarrier(it["barrier"], it["barrierFeatures"])
        )
    }

fun parseBarrier(direction: String?, features: String?): Barrier? {
    if (direction == null) {
        return null
    }

    return Barrier(
        direction = BarrierDirection.parse(direction[0]),
        features = features?.map(Feature.Companion::parse) ?: emptyList()
    )
}
