package com.lalkalol.web

import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.room.service.RoomService
import com.lalkalol.web.dto.ViewBuilder
import com.lalkalol.web.dto.WsStateMessage
import com.lalkalol.web.dto.wsJson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.routing.application
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriodMillis = 15_000
        timeoutMillis = 30_000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/rooms/{code}") {
            val code = call.parameters["code"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing room code"))
            val session = call.sessions.get<PlayerSession>()
            if (session == null || !session.roomCode.equals(code, ignoreCase = true)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }
            val playerId = session.playerUuid()
            call.attributes.put(PlayerIdKey, playerId)
            val uiLocale = LocaleSupport.resolve(call)
            call.attributes.put(UiLocaleKey, uiLocale)

            val hub = application.dependencies.resolve<GameSessionHub>()
            hub.registerConnection(code, playerId, this)
            try {
                val room = application.dependencies.resolve<RoomService>().getRoom(code)
                if (room != null) {
                    val view = ViewBuilder.buildRoomView(room, playerId)
                    send(Frame.Text(wsJson.encodeToString(WsStateMessage(view = view))))
                    hub.onPlayerConnect(code, playerId)
                }
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        hub.handleMessage(code, playerId, frame.readText())
                    }
                }
            } finally {
                hub.unregisterConnection(code, playerId, this)
            }
        }
    }
}
