package com.lalkalol.web.spa

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class SpaNotFoundAdvice(
    private val spaController: SpaController,
) {
    @ExceptionHandler(NoHandlerFoundException::class, NoResourceFoundException::class)
    fun notFound(request: HttpServletRequest): ResponseEntity<String> {
        if (request.method == "GET" && shouldServeSpa(request.requestURI)) {
            return spaController.spaResponse()
        }
        return ResponseEntity.notFound().build()
    }

    private fun shouldServeSpa(uri: String): Boolean {
        if (uri.startsWith("/api") || uri.startsWith("/ws") || uri.startsWith("/health")) return false
        if (uri.startsWith("/actuator")) return false
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3")) return false
        if (uri.startsWith("/assets/") || uri.startsWith("/static/")) return false
        return !uri.contains('.')
    }
}
