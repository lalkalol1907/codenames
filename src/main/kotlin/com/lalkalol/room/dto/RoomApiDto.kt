package com.lalkalol.room.dto

import com.lalkalol.game.dto.GameViewDto

data class CreateRoomRequest(
    val name: String,
    val language: String = "en",
)

data class JoinRoomRequest(
    val code: String,
    val name: String,
)

data class JoinRoomInCodeRequest(
    val name: String,
)

data class SetRoleRequest(
    val team: String? = null,
    val role: String,
)

data class RoomActionResponse(
    val code: String,
    val viewerId: String,
)

data class RoomBootstrapDto(
    val code: String,
    val language: String,
    val needJoin: Boolean,
    val view: RoomViewDto? = null,
)

data class EnumOption(
    val value: String,
    val label: String,
)

data class RoomOptionsDto(
    val teams: List<EnumOption>,
    val roles: List<EnumOption>,
)

data class RoomViewDto(
    val status: String,
    val hostPlayerId: String,
    val players: List<PlayerViewDto>,
    val game: GameViewDto?,
    val viewerId: String,
    val canGiveClue: Boolean,
    val canGuess: Boolean,
    val canEndTurn: Boolean,
    val canStart: Boolean,
    val canRandomizeTeams: Boolean,
)

data class PlayerViewDto(
    val id: String,
    val name: String,
    val team: String?,
    val role: String?,
    val isHost: Boolean,
)
