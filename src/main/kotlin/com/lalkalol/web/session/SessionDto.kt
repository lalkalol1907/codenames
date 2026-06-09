package com.lalkalol.web.session

data class SessionResponse(
    val playerId: String? = null,
    val roomCode: String? = null,
)

data class CsrfResponse(
    val token: String,
)
