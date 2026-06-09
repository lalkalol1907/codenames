package com.lalkalol.web.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RateLimitInterceptor(private val rateLimiter: RateLimiter) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method != "POST") return true
        val key = "ratelimit:${request.remoteAddr}:room-actions"
        if (!rateLimiter.tryAcquire(key)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            return false
        }
        return true
    }
}
