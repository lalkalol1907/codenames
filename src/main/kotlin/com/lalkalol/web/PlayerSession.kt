package com.lalkalol.web

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PlayerSession(
    val playerId: String,
    val roomCode: String,
) {
    fun playerUuid(): UUID = UUID.fromString(playerId)
}
