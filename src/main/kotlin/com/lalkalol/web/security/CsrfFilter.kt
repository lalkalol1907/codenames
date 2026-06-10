package com.lalkalol.web.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CsrfFilter(
    private val csrfSupport: CsrfSupport,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.method == "POST" && request.requestURI.startsWith("/api/") &&
            !isCsrfExempt(request)
        ) {
            if (!csrfSupport.validateApi(request, response)) {
                response.status = HttpStatus.FORBIDDEN.value()
                response.contentType = "application/json"
                response.writer.write("""{"error":"CSRF validation failed"}""")
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun isCsrfExempt(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        // CSRF token issuance endpoint and Discord bootstrap are authentication steps, not state-changes on behalf of an existing session
        if (uri == "/api/csrf" || uri == "/api/discord/bootstrap") return true
        // Requests authenticated via Bearer token don't need CSRF protection
        if (request.getAttribute(com.lalkalol.web.session.PlayerSessionSupport.BEARER_SESSION_ATTR) != null) return true
        return false
    }
}
