package com.lalkalol.web

import com.lalkalol.config.appSettings
import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import java.security.SecureRandom
import java.util.Base64

object Csrf {
    const val HEADER = "X-CSRF-Token"
    const val COOKIE = "csrf_token"

    private val secureRandom = SecureRandom()

    fun token(call: ApplicationCall): String {
        val secure = call.application.appSettings().secureCookies
        call.request.cookies[COOKIE]?.let { return it }
        val token = generateToken()
        call.response.cookies.append(
            Cookie(
                name = COOKIE,
                value = token,
                path = "/",
                httpOnly = false,
                secure = secure,
                extensions = mapOf("SameSite" to "Lax"),
            ),
        )
        return token
    }

    fun validateApi(call: ApplicationCall): Boolean {
        val secure = call.application.appSettings().secureCookies
        val cookieToken = call.request.cookies[COOKIE]
        val headerToken = call.request.headers[HEADER]
        if (cookieToken.isNullOrBlank() || headerToken.isNullOrBlank() || cookieToken != headerToken) {
            call.response.cookies.append(
                Cookie(
                    name = COOKIE,
                    value = "",
                    path = "/",
                    maxAge = 0,
                    httpOnly = false,
                    secure = secure,
                ),
            )
            return false
        }
        return true
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
