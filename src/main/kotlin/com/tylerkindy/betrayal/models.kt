package com.tylerkindy.betrayal

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
enum class CharacterColor {
    RED, YELLOW, GREEN, BLUE, WHITE, PURPLE
}

@Serializable
data class Trait(val value: Int, val index: Short)

@Serializable
data class Player(
    val id: Int,
    val characterName: String,
    val color: CharacterColor,
    val speed: Trait,
    val might: Trait,
    val sanity: Trait,
    val knowledge: Trait
)

@Serializable
data class GameRequest(val name: String)

@Serializable
data class Game(val id: String, val name: String, val players: List<Player>)
