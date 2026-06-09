package com.lalkalol.room.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.lalkalol.room.service.RoomService
import com.lalkalol.room.dto.ViewBuilder
import com.lalkalol.room.dto.WsStateMessage
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.UUID

@Component
class RoomWebSocketHandler(
    private val hub: GameSessionHub,
    private val roomService: RoomService,
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val code = session.attributes["roomCode"] as? String ?: run {
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
        val playerId = session.attributes[GameSessionHub.PLAYER_ID_ATTR] as? UUID ?: run {
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
        hub.registerConnection(code, playerId, session)
        val room = roomService.getRoom(code)
        if (room != null) {
            val view = ViewBuilder.buildRoomView(room, playerId)
            val message = objectMapper.writeValueAsString(WsStateMessage(view = view))
            session.sendMessage(TextMessage(message))
            hub.onPlayerConnect(code, playerId)
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val code = session.attributes["roomCode"] as? String ?: return
        val playerId = session.attributes[GameSessionHub.PLAYER_ID_ATTR] as? UUID ?: return
        hub.handleMessage(code, playerId, message.payload)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val code = session.attributes["roomCode"] as? String ?: return
        val playerId = session.attributes[GameSessionHub.PLAYER_ID_ATTR] as? UUID ?: return
        hub.unregisterConnection(code, playerId, session)
    }
}
