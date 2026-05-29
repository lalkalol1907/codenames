package com.lalkalol.game.model

enum class Team {
    RED,
    BLUE,
}

enum class Role {
    SPYMASTER,
    OPERATIVE,
}

enum class CardType {
    RED,
    BLUE,
    NEUTRAL,
    ASSASSIN,
}

enum class GamePhase {
    CLUE,
    GUESSING,
}

enum class RoomStatus {
    LOBBY,
    PLAYING,
    FINISHED,
}

enum class Language(val code: String) {
    RU("ru"),
    EN("en"),
    ;

    companion object {
        fun fromCode(code: String): Language =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}
