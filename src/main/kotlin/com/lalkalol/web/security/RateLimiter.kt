package com.lalkalol.web.security

interface RateLimiter {
    fun tryAcquire(key: String): Boolean
}
