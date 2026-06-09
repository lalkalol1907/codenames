package com.lalkalol.i18n

import com.lalkalol.common.model.Role
import com.lalkalol.common.model.Team

object PageModel {
    fun teamOptions(locale: UiLocale): List<Map<String, String>> =
        Team.entries.map { team ->
            mapOf("value" to team.name, "label" to Messages.enumLabel(locale, "team", team.name))
        }

    fun roleOptions(locale: UiLocale): List<Map<String, String>> =
        Role.entries.map { role ->
            mapOf("value" to role.name, "label" to Messages.enumLabel(locale, "role", role.name))
        }
}
