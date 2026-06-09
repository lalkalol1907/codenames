package com.lalkalol.room.model

import com.lalkalol.common.model.Language
import com.lalkalol.common.model.Role
import com.lalkalol.common.model.RoomStatus
import com.lalkalol.common.model.Team
import com.lalkalol.game.model.GameState
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
