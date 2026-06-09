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
            request.requestURI != "/api/csrf"
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
}
