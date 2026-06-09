package com.lalkalol.room.web

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.scaling", name = ["redis-enabled"], havingValue = "true")
class RedisRoomEventBus(private val redisTemplate: StringRedisTemplate) : RoomEventBus {

    override fun publish(roomCode: String) {
        redisTemplate.convertAndSend(CHANNEL, roomCode.uppercase())
    }

    companion object {
        const val CHANNEL = "codenames:room-updates"
    }
}
