package com.lalkalol.room.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.lalkalol.common.model.RoomStatus
import com.lalkalol.game.service.GameException
import com.lalkalol.game.service.GameService
import com.lalkalol.i18n.Messages
import com.lalkalol.i18n.UiLocale
import com.lalkalol.room.service.RoomService
import com.lalkalol.game.dto.WsClientMessage
import com.lalkalol.room.dto.ViewBuilder
import com.lalkalol.room.dto.WsErrorMessage
import com.lalkalol.room.dto.WsStateMessage
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Component
class GameSessionHub(
    private val roomService: RoomService,
    private val gameService: GameService,
    private val objectMapper: ObjectMapper,
) {
    private val connections = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()
    private val playerConnectionCounts = ConcurrentHashMap<UUID, AtomicInteger>()
    private val pendingLeave = ConcurrentHashMap<UUID, ScheduledFuture<*>>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "game-session-hub").apply { isDaemon = true }
    }

    fun registerConnection(roomCode: String, playerId: UUID, session: WebSocketSession) {
        pendingLeave.remove(playerId)?.cancel(false)
        playerConnectionCounts.computeIfAbsent(playerId) { AtomicInteger(0) }.incrementAndGet()
        val code = roomCode.uppercase()
        connections.computeIfAbsent(code) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unregisterConnection(roomCode: String, playerId: UUID, session: WebSocketSession) {
        val code = roomCode.uppercase()
        connections[code]?.remove(session)
        if (connections[code]?.isEmpty() == true) {
            connections.remove(code)
        }
        val count = playerConnectionCounts[playerId] ?: return
        if (count.decrementAndGet() <= 0) {
            playerConnectionCounts.remove(playerId)
            scheduleLeave(code, playerId)
        }
    }

    fun onPlayerConnect(roomCode: String, playerId: UUID) {
        pendingLeave.remove(playerId)?.cancel(false)
        broadcast(roomCode.uppercase())
    }

    fun handleMessage(roomCode: String, playerId: UUID, raw: String) {
        val code = roomCode.uppercase()
        try {
            val msg = objectMapper.readValue(raw, WsClientMessage::class.java)
            when (msg) {
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

    fun broadcast(roomCode: String) {
        val code = roomCode.uppercase()
        val room = roomService.getRoom(code) ?: return
        val sessions = connections[code]?.toList().orEmpty()
        for (session in sessions) {
            val playerId = session.attributes[PLAYER_ID_ATTR] as? UUID
            if (playerId != null) {
                val view = ViewBuilder.buildRoomView(room, playerId)
                val message = objectMapper.writeValueAsString(WsStateMessage(view = view))
                sendSafe(session, message)
            }
        }
    }

    private fun scheduleLeave(roomCode: String, playerId: UUID) {
        pendingLeave.remove(playerId)?.cancel(false)
        pendingLeave[playerId] = scheduler.schedule({
            pendingLeave.remove(playerId)
            if (playerConnectionCounts.containsKey(playerId)) return@schedule
            performLeave(roomCode, playerId)
        }, LEAVE_GRACE_MS, TimeUnit.MILLISECONDS)
    }

    private fun performLeave(roomCode: String, playerId: UUID) {
        val code = roomCode.uppercase()
        val room = roomService.getRoom(code) ?: return
        if (room.status != RoomStatus.LOBBY) return
        if (room.players.none { it.id == playerId }) return

        roomService.leaveRoom(code, playerId)
        if (roomService.getRoom(code) != null) {
            broadcast(code)
        }
    }

    private fun sendError(roomCode: String, playerId: UUID, message: String) {
        val code = roomCode.uppercase()
        val sessions = connections[code]?.filter {
            it.attributes[PLAYER_ID_ATTR] == playerId
        }.orEmpty()
        for (session in sessions) {
            val locale = session.attributes[UI_LOCALE_ATTR] as? UiLocale ?: UiLocale.default()
            val translated = Messages.translateException(locale, message)
            val payload = objectMapper.writeValueAsString(WsErrorMessage(message = translated))
            sendSafe(session, payload)
        }
    }

    private fun sendSafe(session: WebSocketSession, message: String) {
        runCatching {
            val safe = if (session is ConcurrentWebSocketSessionDecorator) {
                session
            } else {
                ConcurrentWebSocketSessionDecorator(session, 5000, 64 * 1024)
            }
            if (safe.isOpen) {
                safe.sendMessage(TextMessage(message))
            }
        }
    }

    companion object {
        const val PLAYER_ID_ATTR = "playerId"
        const val UI_LOCALE_ATTR = "uiLocale"
        private const val LEAVE_GRACE_MS = 10_000L
    }
}
