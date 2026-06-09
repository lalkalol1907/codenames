package com.lalkalol.web.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnProperty(prefix = "app.scaling", name = ["redis-enabled"], havingValue = "true")
class RedisRateLimiter(private val redis: StringRedisTemplate) : RateLimiter {

    override fun tryAcquire(key: String): Boolean {
        val count = redis.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redis.expire(key, WINDOW)
        }
        return count <= LIMIT
    }

    companion object {
        private const val LIMIT = 30L
        private val WINDOW = Duration.ofSeconds(60)
    }
}
