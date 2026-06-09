package com.lalkalol.web.security

import com.lalkalol.config.AppProperties
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class CsrfSupport(
    private val appProperties: AppProperties,
) {
    companion object {
        const val HEADER = "X-CSRF-Token"
        const val COOKIE = "csrf_token"
    }

    private val secureRandom = SecureRandom()

    fun token(request: HttpServletRequest, response: HttpServletResponse): String {
        request.cookies?.firstOrNull { it.name == COOKIE }?.value?.let { return it }
        val token = generateToken()
        val cookie = Cookie(COOKIE, token).apply {
            path = "/"
            isHttpOnly = false
            secure = appProperties.secureCookies
            setAttribute("SameSite", "Lax")
        }
        response.addCookie(cookie)
        return token
    }

    fun validateApi(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val cookieToken = request.cookies?.firstOrNull { it.name == COOKIE }?.value
        val headerToken = request.getHeader(HEADER)
        if (cookieToken.isNullOrBlank() || headerToken.isNullOrBlank() || cookieToken != headerToken) {
            val cookie = Cookie(COOKIE, "").apply {
                path = "/"
                maxAge = 0
                isHttpOnly = false
                secure = appProperties.secureCookies
            }
            response.addCookie(cookie)
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
