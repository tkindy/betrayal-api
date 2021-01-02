package com.tylerkindy.betrayal

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

val gameChannels = ConcurrentHashMap<String, Channel<GameUpdate>>()

fun getUpdates(gameId: String): ReceiveChannel<GameUpdate> =
    getOrCreateChannel(gameId)

suspend fun sendUpdate(gameId: String, update: GameUpdate) =
    getOrCreateChannel(gameId).send(update)

private fun getOrCreateChannel(gameId: String) =
    gameChannels.computeIfAbsent(gameId) { Channel() }

// TODO
data class GameUpdate(
    val players: List<Player>
)
