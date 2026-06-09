package com.lalkalol.config

import com.lalkalol.room.web.RoomHandshakeInterceptor
import com.lalkalol.room.web.RoomWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val roomWebSocketHandler: RoomWebSocketHandler,
    private val roomHandshakeInterceptor: RoomHandshakeInterceptor,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(roomWebSocketHandler, "/ws/rooms/{code}")
            .addInterceptors(HttpSessionHandshakeInterceptor(), roomHandshakeInterceptor)
            .setAllowedOrigins("*")
    }
}
