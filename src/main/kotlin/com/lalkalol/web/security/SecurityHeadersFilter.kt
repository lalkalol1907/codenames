package com.lalkalol.web.security

import com.lalkalol.config.AppProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SecurityHeadersFilter(
    private val appProperties: AppProperties,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val frameAncestors = appProperties.embed.frameAncestors.trim()
        if (frameAncestors.isNotEmpty()) {
            response.setHeader("Content-Security-Policy", "frame-ancestors $frameAncestors")
        } else {
            response.setHeader("X-Frame-Options", "DENY")
        }
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
        if (appProperties.isProduction) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }
        filterChain.doFilter(request, response)
    }
}
