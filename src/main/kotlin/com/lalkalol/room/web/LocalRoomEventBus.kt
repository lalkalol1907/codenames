package com.lalkalol.room.web

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "app.scaling",
    name = ["redis-enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class LocalRoomEventBus(@Lazy private val hub: GameSessionHub) : RoomEventBus {
    override fun publish(roomCode: String) {
        hub.broadcastLocal(roomCode)
    }
}
