package com.lalkalol.i18n

import java.util.Locale

enum class UiLocale(val code: String) {
    RU("ru"),
    EN("en"),
    ;

    fun toJavaLocale(): Locale = Locale.forLanguageTag(code)

    companion object {
        fun fromCode(code: String): UiLocale? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        fun default(): UiLocale = EN
    }
}
