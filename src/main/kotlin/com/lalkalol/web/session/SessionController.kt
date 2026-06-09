package com.lalkalol.web.session

import com.lalkalol.web.security.CsrfSupport
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class SessionController(
    private val csrfSupport: CsrfSupport,
) {
    @GetMapping("/csrf")
    fun csrf(request: HttpServletRequest, response: HttpServletResponse): CsrfResponse {
        val token = csrfSupport.token(request, response)
        return CsrfResponse(token)
    }

    @GetMapping("/session")
    fun session(request: HttpServletRequest): SessionResponse {
        val session = PlayerSessionSupport.get(request.getSession(false))
        return if (session == null) {
            SessionResponse()
        } else {
            SessionResponse(session.playerId, session.roomCode)
        }
    }
}
