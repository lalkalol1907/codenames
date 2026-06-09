package com.lalkalol.web.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
@ConditionalOnProperty(
    prefix = "app.scaling",
    name = ["redis-enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class InMemoryRateLimiter : RateLimiter {
    private val buckets = ConcurrentHashMap<String, RateBucket>()

    override fun tryAcquire(key: String): Boolean {
        val now = System.currentTimeMillis()
        val bucket = buckets.compute(key) { _, existing ->
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                RateBucket(now, AtomicInteger(0))
            } else {
                existing
            }
        }!!
        return bucket.count.incrementAndGet() <= LIMIT
    }

    private data class RateBucket(val windowStart: Long, val count: AtomicInteger)

    companion object {
        private const val LIMIT = 30
        private const val WINDOW_MS = 60_000L
    }
}
