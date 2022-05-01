package com.tylerkindy.betrayal

import com.tylerkindy.betrayal.db.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

val lobbyStates = ConcurrentHashMap<String, LobbyState>()

data class LobbyState(val players: List<PlayerState> = listOf())
data class PlayerState(val name: String, val session: DefaultWebSocketSession)


val lobbyUpdateManager = UpdateManager { lobbyId ->
    lobbyStates[lobbyId] ?: throw IllegalArgumentException("No lobby with ID $lobbyId")
}
val gameUpdateManager = UpdateManager { gameId ->
    GameUpdate(
        rooms = getRooms(gameId),
        players = getPlayers(gameId),
        roomStack = getRoomStackState(gameId),
        drawnCard = getDrawnCard(gameId),
        latestRoll = getLatestRoll(gameId),
        monsters = getMonsters(gameId)
    )
}

@Serializable
data class LobbyUpdate(
    val hostId: Int,
    val players: List<LobbyPlayer>
)

@Serializable
data class GameUpdate(
    val rooms: List<Room>,
    val players: List<Player>,
    val roomStack: RoomStackResponse,
    val drawnCard: Card?,
    val latestRoll: DiceRoll?,
    val monsters: List<Monster>
)

class UpdateManager<T>(private val buildUpdate: (String) -> T) {
    private val flows = ConcurrentHashMap<String, MutableSharedFlow<T>>()

    fun getUpdates(id: String): SharedFlow<T> =
        getOrCreateFlow(id)

    suspend fun sendUpdate(id: String) {
        getOrCreateFlow(id).emit(buildUpdate(id))
    }

    private fun getOrCreateFlow(id: String) =
        flows.computeIfAbsent(id) { MutableSharedFlow(replay = 1) }
}

inline fun <reified T> Route.addUpdateRoute(
    updateManager: UpdateManager<T>,
    idParam: String
) {
    webSocket {
        val id = call.parameters[idParam]!!
        sendUpdates(updateManager, id)
    }
}

suspend inline fun <reified T> WebSocketSession.sendUpdates(
    updateManager: UpdateManager<T>,
    id: String
) {
    updateManager.getUpdates(id).collect { update ->
        send(Frame.Text(Json.encodeToString(update)))
    }
}
