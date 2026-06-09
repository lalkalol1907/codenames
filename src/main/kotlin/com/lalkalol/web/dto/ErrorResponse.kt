package com.lalkalol.web.dto

data class ErrorResponse(
    val error: String,
    val needJoin: Boolean = false,
)
