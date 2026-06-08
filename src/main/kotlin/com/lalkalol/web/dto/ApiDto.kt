package com.lalkalol.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val needJoin: Boolean = false,
)

@Serializable
data class CreateRoomRequest(
    val name: String,
    val language: String = "en",
)

@Serializable
data class JoinRoomRequest(
    val code: String,
    val name: String,
)

@Serializable
data class JoinRoomInCodeRequest(
    val name: String,
)

@Serializable
data class SetRoleRequest(
    val team: String,
    val role: String,
)

@Serializable
data class SetLocaleRequest(
    val locale: String,
)

@Serializable
data class RoomActionResponse(
    val code: String,
    val viewerId: String,
)

@Serializable
data class SessionResponse(
    val playerId: String? = null,
    val roomCode: String? = null,
)

@Serializable
data class CsrfResponse(
    val token: String,
)

@Serializable
data class RoomBootstrapDto(
    val code: String,
    val language: String,
    val needJoin: Boolean,
    val view: RoomViewDto? = null,
)

@Serializable
data class EnumOption(
    val value: String,
    val label: String,
)

@Serializable
data class RoomOptionsDto(
    val teams: List<EnumOption>,
    val roles: List<EnumOption>,
)
