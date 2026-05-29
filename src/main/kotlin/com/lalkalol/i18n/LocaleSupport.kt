package com.lalkalol.i18n

import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object LocaleSupport {
    const val COOKIE_NAME = "ui_locale"
    private const val COOKIE_MAX_AGE = 365 * 24 * 3600

    fun resolve(call: ApplicationCall): UiLocale {
        call.request.cookies[COOKIE_NAME]?.let { UiLocale.fromCode(it) }?.let { return it }
        val accept = call.request.headers["Accept-Language"].orEmpty()
        if (accept.contains("ru", ignoreCase = true)) return UiLocale.RU
        return UiLocale.default()
    }

    fun setCookie(call: ApplicationCall, locale: UiLocale) {
        call.response.cookies.append(
            Cookie(
                name = COOKIE_NAME,
                value = locale.code,
                path = "/",
                maxAge = COOKIE_MAX_AGE,
                httpOnly = false,
                extensions = mapOf("SameSite" to "Lax"),
            ),
        )
    }

    fun encodeRedirectParam(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
