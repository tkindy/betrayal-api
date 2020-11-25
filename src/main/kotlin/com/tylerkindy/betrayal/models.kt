package com.tylerkindy.betrayal

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
enum class CharacterColor {
    RED, YELLOW, GREEN, BLUE, WHITE, PURPLE
}

@Serializable
data class Trait(val value: Int, val index: Int)

@Serializable
data class GridLoc(val gridX: Int, val gridY: Int)

@Serializable
data class Player(
    val id: Int,
    val characterName: String,
    val color: CharacterColor,
    val loc: GridLoc,
    val speed: Trait,
    val might: Trait,
    val sanity: Trait,
    val knowledge: Trait
)

@Serializable
enum class Floor(val defEncoding: Char) {
    BASEMENT('B'),
    GROUND('G'),
    UPPER('U'),
    ROOF('R');

    companion object {
        private val index = values().associateBy { it.defEncoding }

        fun parse(defEncoding: Char) = index[defEncoding]
            ?: throw IllegalArgumentException("No floor for encoding '$defEncoding'")
    }
}

@Serializable
enum class Direction(val defEncoding: Char) {
    NORTH('N'),
    SOUTH('S'),
    EAST('E'),
    WEST('W');

    companion object {
        private val index = values().associateBy { it.defEncoding }

        fun parse(defEncoding: Char) = index[defEncoding]
            ?: throw IllegalArgumentException("No direction for encoding '$defEncoding'")
    }
}

@Serializable
enum class Feature(val defEncoding: Char) {
    EVENT('E'),
    ITEM('I'),
    OMEN('O'),
    ANY('A'),
    DUMBWAITER('D');

    companion object {
        private val index = values().associateBy { it.defEncoding }

        fun parse(defEncoding: Char) = index[defEncoding]
            ?: throw IllegalArgumentException("No feature for encoding '$defEncoding'")
    }
}

@Serializable
enum class BarrierDirection(val defEncoding: Char) {
    VERTICAL('V'),
    HORIZONTAL('H');

    companion object {
        private val index = values().associateBy { it.defEncoding }

        fun parse(defEncoding: Char) = index[defEncoding]
            ?: throw IllegalArgumentException("No barrier direction for encoding '$defEncoding'")
    }
}

@Serializable
data class Barrier(
    val direction: BarrierDirection,
    val features: List<Feature>
)

@Serializable
data class Room(
    val id: Int,
    val name: String,
    val floors: Set<Floor>,
    val doors: Set<Direction>,
    val features: List<Feature>,
    val description: String?,
    val barrier: Barrier?
)

@Serializable
data class GameRequest(val name: String, val numPlayers: Int)

@Serializable
data class Game(val id: String, val name: String, val players: List<Player>)
