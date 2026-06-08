package com.lalkalol.i18n

import java.text.MessageFormat
import java.util.ResourceBundle

object Messages {
    private const val BUNDLE = "i18n.messages"

    private val exceptionKeys = mapOf(
        "Room not found" to "error.room_not_found",
        "Game already started" to "error.game_already_started",
        "Cannot change role after game started" to "error.cannot_change_role",
        "Only host can start the game" to "error.only_host_start",
        "Only host can randomize teams" to "error.only_host_randomize",
        "Exactly 4 players required for random teams" to "error.exactly_four_players",
        "Player not in room" to "error.player_not_in_room",
        "Role already taken" to "error.role_taken",
        "At least 4 players required" to "error.min_players",
        "Each team needs exactly 1 spymaster and 1 operative" to "error.team_roles",
        "All players must choose team and role" to "error.all_roles_required",
        "Could not generate room code" to "error.room_code_generation",
        "Game is over" to "error.game_over",
        "Not clue phase" to "error.not_clue_phase",
        "Not guessing phase" to "error.not_guessing_phase",
        "Only spymaster can give clues" to "error.only_spymaster_clue",
        "Only operatives can guess" to "error.only_operative_guess",
        "Only operatives can end turn" to "error.only_operative_end",
        "Not your team's turn" to "error.not_your_turn",
        "Clue word cannot be empty" to "error.clue_empty",
        "Clue count must be at least 1" to "error.clue_count",
        "Clue cannot match a word on the board" to "error.clue_on_board",
        "Invalid card position" to "error.invalid_card",
        "Card already revealed" to "error.card_revealed",
        "Game state changed" to "error.game_state_changed",
        "Game is not in progress" to "error.game_not_in_progress",
        "Player not found" to "error.player_not_found",
        "Unknown message type" to "error.unknown_message",
        "Game error" to "error.game_error",
        "Unexpected error" to "error.unexpected",
    )

    fun t(locale: UiLocale, key: String, vararg args: Any): String {
        val raw = runCatching {
            ResourceBundle.getBundle(BUNDLE, locale.toJavaLocale()).getString(key)
        }.getOrElse { key }
        if (args.isEmpty()) return raw
        return MessageFormat.format(raw, *args)
    }

    fun enumLabel(locale: UiLocale, prefix: String, value: String): String =
        t(locale, "$prefix.$value")

    fun translateException(locale: UiLocale, message: String?): String {
        if (message.isNullOrBlank()) return t(locale, "error.unexpected")
        val key = exceptionKeys[message] ?: return message
        return t(locale, key)
    }

    fun allMessages(locale: UiLocale): Map<String, String> {
        val bundle = ResourceBundle.getBundle(BUNDLE, locale.toJavaLocale())
        return bundle.keys.asSequence().associateWith { key ->
            bundle.getString(key)
        }
    }
}
