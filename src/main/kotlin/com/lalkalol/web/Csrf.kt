package com.lalkalol.web

import com.lalkalol.config.appSettings
import io.ktor.http.Cookie
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import java.security.SecureRandom
import java.util.Base64

object Csrf {
    const val PARAM = "_csrf"
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
                httpOnly = true,
                secure = secure,
                extensions = mapOf("SameSite" to "Lax"),
            ),
        )
        return token
    }

    fun validate(call: ApplicationCall, params: Parameters): Boolean {
        val secure = call.application.appSettings().secureCookies
        val cookieToken = call.request.cookies[COOKIE]
        val paramToken = params[PARAM]
        if (cookieToken.isNullOrBlank() || paramToken.isNullOrBlank() || cookieToken != paramToken) {
            call.response.cookies.append(
                Cookie(
                    name = COOKIE,
                    value = "",
                    path = "/",
                    maxAge = 0,
                    httpOnly = true,
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
