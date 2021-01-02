package com.tylerkindy.betrayal

import com.tylerkindy.betrayal.db.getDrawnCard
import com.tylerkindy.betrayal.db.getPlayers
import com.tylerkindy.betrayal.db.getRoomStackState
import com.tylerkindy.betrayal.db.getRooms
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable

private val gameUpdateFlows = ConcurrentHashMap<String, MutableSharedFlow<GameUpdate>>()

fun getUpdates(gameId: String): SharedFlow<GameUpdate> =
    getOrCreateFlow(gameId)

suspend fun sendUpdate(gameId: String) =
    getOrCreateFlow(gameId).emit(buildUpdate(gameId))

private fun getOrCreateFlow(gameId: String) =
    gameUpdateFlows.computeIfAbsent(gameId) { MutableSharedFlow() }

private fun buildUpdate(gameId: String) =
    GameUpdate(
    rooms = getRooms(gameId),
    players = getPlayers(gameId),
    roomStack = getRoomStackState(gameId),
    drawnCard = getDrawnCard(gameId)
)

@Serializable
data class GameUpdate(
    val rooms: List<Room>,
    val players: List<Player>,
    val roomStack: RoomStackResponse,
    val drawnCard: Card?
)
