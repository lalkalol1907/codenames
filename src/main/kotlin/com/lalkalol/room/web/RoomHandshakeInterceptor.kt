package com.lalkalol.room.web

import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.web.session.PlayerSessionSupport
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class RoomHandshakeInterceptor(
    private val localeSupport: LocaleSupport,
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest
            ?: return false
        val uri = servletRequest.requestURI
        val code = extractRoomCode(uri) ?: return false
        val session = PlayerSessionSupport.get(servletRequest.getSession(false))
        if (session == null || !session.roomCode.equals(code, ignoreCase = true)) {
            return false
        }
        attributes["roomCode"] = code.uppercase()
        attributes[GameSessionHub.PLAYER_ID_ATTR] = session.playerUuid()
        attributes[GameSessionHub.UI_LOCALE_ATTR] = localeSupport.resolve(servletRequest)
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) = Unit

    private fun extractRoomCode(uri: String): String? {
        val prefix = "/ws/rooms/"
        if (!uri.startsWith(prefix)) return null
        val code = uri.removePrefix(prefix).trimEnd('/')
        return code.ifBlank { null }
    }
}
