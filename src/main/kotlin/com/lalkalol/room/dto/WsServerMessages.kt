package com.lalkalol.room.dto

data class WsStateMessage(
    val type: String = "state",
    val view: RoomViewDto,
)

data class WsErrorMessage(
    val type: String = "error",
    val message: String,
)
