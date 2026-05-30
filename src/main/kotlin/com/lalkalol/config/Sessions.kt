package com.lalkalol.config

import com.lalkalol.web.PlayerSession
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication

fun Application.configureSessions() {
    val settings = appSettings()
    val secret = settings.sessionSecret.toByteArray(Charsets.UTF_8)

    install(Sessions) {
        cookie<PlayerSession>("player_session") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = settings.secureCookies
            cookie.extensions["SameSite"] = "Lax"
            transform(SessionTransportTransformerMessageAuthentication(secret))
        }
    }
}
