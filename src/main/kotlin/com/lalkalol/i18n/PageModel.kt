package com.lalkalol.i18n

import com.lalkalol.game.model.Role
import com.lalkalol.game.model.Team
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondRedirect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private val json = Json { encodeDefaults = true }

object PageModel {
    fun base(call: ApplicationCall, extra: Map<String, Any?> = emptyMap()): Map<String, Any?> {
        val locale = LocaleSupport.resolve(call)
        val redirect = call.request.local.uri
        return mapOf(
            "locale" to locale.code,
            "redirect" to redirect,
            "msg" to MsgMethod(locale),
            "i18nJson" to PageModel.clientI18nJson(locale),
        ) + extra
    }

    fun teamOptions(locale: UiLocale): List<Map<String, String>> =
        Team.entries.map { team ->
            mapOf("value" to team.name, "label" to Messages.enumLabel(locale, "team", team.name))
        }

    fun roleOptions(locale: UiLocale): List<Map<String, String>> =
        Role.entries.map { role ->
            mapOf("value" to role.name, "label" to Messages.enumLabel(locale, "role", role.name))
        }

    fun clientI18nJson(locale: UiLocale): String = json.encodeToString(
        buildJsonObject {
            put("host", Messages.t(locale, "lobby.host"))
            put("choosingRole", Messages.t(locale, "lobby.choosing_role"))
            put("startGame", Messages.t(locale, "lobby.start_game"))
            put("startHint", Messages.t(locale, "lobby.start_hint"))
            put("waitingHint", Messages.t(locale, "lobby.waiting_hint"))
            put("randomizeTeams", Messages.t(locale, "lobby.randomize_teams"))
            put("randomizeHint", Messages.t(locale, "lobby.randomize_hint"))
            put("gameOver", Messages.t(locale, "game.over"))
            put("wins", Messages.t(locale, "game.wins"))
            put("teamWins", Messages.t(locale, "game.team_wins"))
            put("turn", Messages.t(locale, "game.turn"))
            put("redLeft", Messages.t(locale, "game.red_left"))
            put("blueLeft", Messages.t(locale, "game.blue_left"))
            put("phase", Messages.t(locale, "game.phase"))
            put("clue", Messages.t(locale, "game.clue"))
            put("waitingClue", Messages.t(locale, "game.waiting_clue"))
            put("waitingYourClue", Messages.t(locale, "game.waiting_your_clue"))
            put("clueWord", Messages.t(locale, "game.clue_word"))
            put("count", Messages.t(locale, "game.count"))
            put("giveClue", Messages.t(locale, "game.give_clue"))
            put("endTurn", Messages.t(locale, "game.end_turn"))
            putJsonObject("teams") {
                Team.entries.forEach { put(it.name, Messages.enumLabel(locale, "team", it.name)) }
            }
            putJsonObject("roles") {
                Role.entries.forEach { put(it.name, Messages.enumLabel(locale, "role", it.name)) }
            }
            putJsonObject("phases") {
                put("CLUE", Messages.enumLabel(locale, "phase", "CLUE"))
                put("GUESSING", Messages.enumLabel(locale, "phase", "GUESSING"))
            }
        },
    )

    suspend fun ApplicationCall.redirectWithError(path: String, errorKey: String) {
        val locale = LocaleSupport.resolve(this)
        val message = Messages.t(locale, errorKey)
        respondRedirect("$path?error=${LocaleSupport.encodeRedirectParam(message)}")
    }

    suspend fun ApplicationCall.redirectWithException(path: String, message: String?) {
        val locale = LocaleSupport.resolve(this)
        val translated = Messages.translateException(locale, message)
        respondRedirect("$path?error=${LocaleSupport.encodeRedirectParam(translated)}")
    }
}
