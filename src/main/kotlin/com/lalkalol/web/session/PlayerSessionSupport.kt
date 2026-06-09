package com.lalkalol.web.session

import jakarta.servlet.http.HttpSession
import java.util.UUID

data class PlayerSession(
    val playerId: String,
    val roomCode: String,
) {
    fun playerUuid(): UUID = UUID.fromString(playerId)
}

object PlayerSessionSupport {
    const val PLAYER_ID_ATTR = "playerId"
    const val ROOM_CODE_ATTR = "roomCode"

    fun get(session: HttpSession?): PlayerSession? {
        if (session == null) return null
        val playerId = session.getAttribute(PLAYER_ID_ATTR) as? String ?: return null
        val roomCode = session.getAttribute(ROOM_CODE_ATTR) as? String ?: return null
        return PlayerSession(playerId, roomCode)
    }

    fun set(session: HttpSession, playerId: String, roomCode: String) {
        session.setAttribute(PLAYER_ID_ATTR, playerId)
        session.setAttribute(ROOM_CODE_ATTR, roomCode)
    }

    fun clear(session: HttpSession) {
        session.removeAttribute(PLAYER_ID_ATTR)
        session.removeAttribute(ROOM_CODE_ATTR)
    }
}
