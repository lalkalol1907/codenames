package com.lalkalol.i18n.web

import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.i18n.Messages
import com.lalkalol.i18n.UiLocale
import com.lalkalol.i18n.dto.SetLocaleRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class I18nController(
    private val localeSupport: LocaleSupport,
) {
    @GetMapping("/i18n")
    fun i18n(
        request: HttpServletRequest,
        @RequestParam(required = false) locale: String?,
    ): Map<String, String> {
        val resolved = UiLocale.fromCode(locale ?: "") ?: localeSupport.resolve(request)
        return Messages.allMessages(resolved)
    }

    @PostMapping("/locale")
    fun setLocale(
        response: HttpServletResponse,
        @RequestBody body: SetLocaleRequest,
    ): ResponseEntity<Void> {
        val locale = UiLocale.fromCode(body.locale) ?: UiLocale.default()
        localeSupport.setCookie(response, locale)
        return ResponseEntity.noContent().build()
    }
}
