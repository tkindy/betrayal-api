package com.tylerkindy.betrayal

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.ConcurrentHashMap

val gameChannels = ConcurrentHashMap<String, MutableSharedFlow<GameUpdate>>()

fun getUpdates(gameId: String): SharedFlow<GameUpdate> =
    getOrCreateFlow(gameId)

suspend fun sendUpdate(gameId: String, update: GameUpdate) =
    getOrCreateFlow(gameId).emit(update)

private fun getOrCreateFlow(gameId: String) =
    gameChannels.computeIfAbsent(gameId) { MutableSharedFlow() }

// TODO
data class GameUpdate(
    val players: List<Player>
)
