package com.lalkalol.room.web

import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.web.security.AuthTokenService
import com.lalkalol.web.session.PlayerSessionSupport
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.UUID

@Component
class RoomHandshakeInterceptor(
    private val localeSupport: LocaleSupport,
    private val authTokenService: AuthTokenService,
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

        val tokenParam = servletRequest.getParameter("token")
        if (tokenParam != null) {
            return validateViaToken(tokenParam, code, servletRequest, attributes)
        }

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

    private fun validateViaToken(
        token: String,
        uriCode: String,
        servletRequest: HttpServletRequest,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val payload = authTokenService.validate(token) ?: return false
        if (!payload.roomCode.equals(uriCode, ignoreCase = true)) return false
        attributes["roomCode"] = uriCode.uppercase()
        attributes[GameSessionHub.PLAYER_ID_ATTR] = UUID.fromString(payload.playerId)
        attributes[GameSessionHub.UI_LOCALE_ATTR] = localeSupport.resolve(servletRequest)
        return true
    }

    private fun extractRoomCode(uri: String): String? {
        val prefix = "/ws/rooms/"
        if (!uri.startsWith(prefix)) return null
        val raw = uri.removePrefix(prefix).substringBefore('?').trimEnd('/')
        return raw.ifBlank { null }
    }
}
