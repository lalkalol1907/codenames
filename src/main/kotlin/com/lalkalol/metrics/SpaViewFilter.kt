package com.lalkalol.metrics

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val SPA_ROUTES = setOf(
    "/",
    Regex("/rooms/[A-Z0-9]+"),
    Regex("/rooms/[A-Z0-9]+/game"),
)

@Component
class SpaViewFilter(private val metricsService: MetricsService) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return uri.startsWith("/actuator") || uri.startsWith("/api") || uri.startsWith("/ws")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        filterChain.doFilter(request, response)

        // Count only GET requests that returned 200 for SPA routes
        if (request.method == "GET" && response.status == 200) {
            val path = request.requestURI
            if (isSpaRoute(path)) {
                metricsService.incrementSpaView(path)
            }
        }
    }

    private fun isSpaRoute(path: String): Boolean {
        if (path in setOf("/")) return true
        return path.matches(Regex("/rooms/[A-Z0-9]+")) ||
            path.matches(Regex("/rooms/[A-Z0-9]+/game"))
    }
}
