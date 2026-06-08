package com.lalkalol.web

import com.lalkalol.i18n.Messages
import com.lalkalol.i18n.UiLocale
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.service.GameException
import com.lalkalol.game.service.GameService
import com.lalkalol.room.service.RoomService
import com.lalkalol.web.dto.ViewBuilder
import com.lalkalol.web.dto.WsClientMessage
import com.lalkalol.web.dto.WsErrorMessage
import com.lalkalol.web.dto.WsStateMessage
import com.lalkalol.web.dto.wsJson
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GameSessionHub(
    private val roomService: RoomService,
    private val gameService: GameService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connections = ConcurrentHashMap<String, MutableSet<WebSocketServerSession>>()
    private val playerConnectionCounts = ConcurrentHashMap<UUID, AtomicInteger>()
    private val pendingLeave = ConcurrentHashMap<UUID, Job>()
    private val mutex = Mutex()
    private val json = wsJson

    suspend fun registerConnection(roomCode: String, playerId: UUID, session: WebSocketServerSession) {
        pendingLeave.remove(playerId)?.cancel()
        playerConnectionCounts.computeIfAbsent(playerId) { AtomicInteger(0) }.incrementAndGet()
        val code = roomCode.uppercase()
        mutex.withLock {
            connections.computeIfAbsent(code) { mutableSetOf() }.add(session)
        }
    }

    suspend fun unregisterConnection(roomCode: String, playerId: UUID, session: WebSocketServerSession) {
        val code = roomCode.uppercase()
        mutex.withLock {
            connections[code]?.remove(session)
            if (connections[code]?.isEmpty() == true) {
                connections.remove(code)
            }
        }
        val count = playerConnectionCounts[playerId] ?: return
        if (count.decrementAndGet() <= 0) {
            playerConnectionCounts.remove(playerId)
            scheduleLeave(code, playerId)
        }
    }

    suspend fun onPlayerConnect(roomCode: String, playerId: UUID) {
        pendingLeave.remove(playerId)?.cancel()
        broadcast(roomCode.uppercase())
    }

    suspend fun handleMessage(roomCode: String, playerId: UUID, raw: String) {
        val code = roomCode.uppercase()
        try {
            when (val msg = json.decodeFromString<WsClientMessage>(raw)) {
                is WsClientMessage.GiveClue -> gameService.giveClue(code, playerId, msg.word, msg.count)
                is WsClientMessage.Guess -> gameService.guess(code, playerId, msg.index)
                is WsClientMessage.EndTurn -> gameService.endTurn(code, playerId)
            }
            broadcast(code)
        } catch (e: GameException) {
            sendError(code, playerId, e.message ?: "Game error")
        } catch (e: Exception) {
            sendError(code, playerId, e.message ?: "Unexpected error")
        }
    }

    suspend fun broadcast(roomCode: String) {
        val code = roomCode.uppercase()
        val room = roomService.getRoom(code) ?: return
        val sessions = mutex.withLock { connections[code]?.toList().orEmpty() }
        for (session in sessions) {
            val playerId = session.call.attributes.getOrNull(PlayerIdKey)
            if (playerId != null) {
                val view = ViewBuilder.buildRoomView(room, playerId)
                val message = json.encodeToString(WsStateMessage(view = view))
                runCatching { session.send(Frame.Text(message)) }
            }
        }
    }

    private fun scheduleLeave(roomCode: String, playerId: UUID) {
        pendingLeave.remove(playerId)?.cancel()
        pendingLeave[playerId] = scope.launch {
            delay(LEAVE_GRACE_MS)
            pendingLeave.remove(playerId)
            if (playerConnectionCounts.containsKey(playerId)) return@launch
            performLeave(roomCode, playerId)
        }
    }

    private suspend fun performLeave(roomCode: String, playerId: UUID) {
        val code = roomCode.uppercase()
        val room = roomService.getRoom(code) ?: return
        if (room.status != RoomStatus.LOBBY) return
        if (room.players.none { it.id == playerId }) return

        roomService.leaveRoom(code, playerId)
        if (roomService.getRoom(code) != null) {
            broadcast(code)
        }
    }

    private suspend fun sendError(roomCode: String, playerId: UUID, message: String) {
        val code = roomCode.uppercase()
        val sessions = mutex.withLock {
            connections[code]?.filter {
                it.call.attributes.getOrNull(PlayerIdKey) == playerId
            }.orEmpty()
        }
        for (session in sessions) {
            val locale = session.call.attributes.getOrNull(UiLocaleKey) ?: UiLocale.default()
            val translated = Messages.translateException(locale, message)
            val localizedPayload = json.encodeToString(WsErrorMessage(message = translated))
            runCatching { session.send(Frame.Text(localizedPayload)) }
        }
    }

    companion object {
        private const val LEAVE_GRACE_MS = 10_000L
    }
}

val PlayerIdKey = io.ktor.util.AttributeKey<UUID>("PlayerId")
val UiLocaleKey = io.ktor.util.AttributeKey<UiLocale>("UiLocale")
