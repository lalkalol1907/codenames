package com.lalkalol.scaling

import com.lalkalol.web.security.RedisRateLimiter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class RedisRateLimiterTest {

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var valueOps: ValueOperations<String, String>

    private fun limiter(): RedisRateLimiter {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        return RedisRateLimiter(redisTemplate)
    }

    @Test
    fun `first request in window sets TTL and is allowed`() {
        `when`(valueOps.increment("key")).thenReturn(1L)
        val limiter = limiter()

        val result = limiter.tryAcquire("key")

        assertTrue(result)
        verify(redisTemplate).expire(eq("key"), any(Duration::class.java))
    }

    @Test
    fun `request within limit is allowed without resetting TTL`() {
        `when`(valueOps.increment("key")).thenReturn(15L)
        val limiter = limiter()

        val result = limiter.tryAcquire("key")

        assertTrue(result)
        verify(redisTemplate, never()).expire(any(String::class.java), any(Duration::class.java))
    }

    @Test
    fun `request over limit is rejected`() {
        `when`(valueOps.increment("key")).thenReturn(31L)

        val result = limiter().tryAcquire("key")

        assertFalse(result)
    }

    @Test
    fun `request at exactly the limit is allowed`() {
        `when`(valueOps.increment("key")).thenReturn(30L)

        val result = limiter().tryAcquire("key")

        assertTrue(result)
    }
}
