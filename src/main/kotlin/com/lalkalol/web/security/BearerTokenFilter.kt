package com.lalkalol.web.security

import com.lalkalol.web.session.PlayerSession
import com.lalkalol.web.session.PlayerSessionSupport
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(5)
class BearerTokenFilter(
    private val authTokenService: AuthTokenService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            val payload = authTokenService.validate(token)
            if (payload != null) {
                request.setAttribute(
                    PlayerSessionSupport.BEARER_SESSION_ATTR,
                    PlayerSession(payload.playerId, payload.roomCode),
                )
            }
        }
        filterChain.doFilter(request, response)
    }
}
