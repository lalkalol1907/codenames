package com.lalkalol.room.model

import com.lalkalol.game.model.GameState
import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import java.util.UUID

data class Player(
    val id: UUID,
    val roomId: UUID,
    val name: String,
    val team: Team?,
    val role: Role?,
    val isHost: Boolean,
)

data class Room(
    val id: UUID,
    val code: String,
    val language: Language,
    val hostPlayerId: UUID,
    val status: RoomStatus,
    val players: List<Player>,
    val game: GameState?,
)
