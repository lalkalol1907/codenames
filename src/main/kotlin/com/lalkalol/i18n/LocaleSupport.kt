package com.lalkalol.i18n

import com.lalkalol.config.AppProperties
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class LocaleSupport(
    private val appProperties: AppProperties,
) {
    companion object {
        const val COOKIE_NAME = "ui_locale"
        private const val COOKIE_MAX_AGE = 365 * 24 * 3600
    }

    fun resolve(request: HttpServletRequest): UiLocale {
        request.cookies?.firstOrNull { it.name == COOKIE_NAME }?.value
            ?.let { UiLocale.fromCode(it) }
            ?.let { return it }
        val accept = request.getHeader("Accept-Language").orEmpty()
        if (accept.contains("ru", ignoreCase = true)) return UiLocale.RU
        return UiLocale.default()
    }

    fun setCookie(response: HttpServletResponse, locale: UiLocale) {
        val cookie = Cookie(COOKIE_NAME, locale.code).apply {
            path = "/"
            maxAge = COOKIE_MAX_AGE
            isHttpOnly = false
            secure = appProperties.secureCookies
            setAttribute("SameSite", "Lax")
        }
        response.addCookie(cookie)
    }
}
