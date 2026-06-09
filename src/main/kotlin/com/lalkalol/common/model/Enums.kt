package com.lalkalol.common.model

enum class Team {
    RED,
    BLUE,
}

enum class Role {
    SPYMASTER,
    OPERATIVE,
    SPECTATOR,
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
