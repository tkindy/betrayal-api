package com.tylerkindy.betrayal

import kotlinx.serialization.Serializable

@Serializable
data class GameRequest(val name: String)

@Serializable
data class Game(val id: String, val name: String)
