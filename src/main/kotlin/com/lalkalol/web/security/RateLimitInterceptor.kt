package com.lalkalol.web.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class RateLimitInterceptor : HandlerInterceptor {
    private val buckets = ConcurrentHashMap<String, RateBucket>()

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method != "POST") return true
        val key = "${request.remoteAddr}:room-actions"
        val now = System.currentTimeMillis()
        val bucket = buckets.compute(key) { _, existing ->
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                RateBucket(now, AtomicInteger(0))
            } else {
                existing
            }
        }!!
        if (bucket.count.incrementAndGet() > LIMIT) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            return false
        }
        return true
    }

    private data class RateBucket(
        val windowStart: Long,
        val count: AtomicInteger,
    )

    companion object {
        private const val LIMIT = 30
        private const val WINDOW_MS = 60_000L
    }
}
