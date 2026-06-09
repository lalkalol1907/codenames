package com.lalkalol.room.web

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
@ConditionalOnProperty(prefix = "app.scaling", name = ["redis-enabled"], havingValue = "true")
class RedisPresenceTracker(private val redis: StringRedisTemplate) : PresenceTracker {

    override fun increment(playerId: UUID) {
        val key = key(playerId)
        redis.opsForValue().increment(key)
        redis.expire(key, TTL)
    }

    override fun decrement(playerId: UUID): Long {
        val key = key(playerId)
        val result = redis.opsForValue().decrement(key) ?: 0L
        if (result <= 0L) redis.delete(key)
        return maxOf(result, 0L)
    }

    override fun count(playerId: UUID): Long =
        redis.opsForValue().get(key(playerId))?.toLongOrNull() ?: 0L

    private fun key(playerId: UUID) = "presence:$playerId"

    companion object {
        private val TTL = Duration.ofMinutes(60)
    }
}
