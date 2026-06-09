package com.lalkalol.game.dto

data class CardView(
    val position: Int,
    val word: String,
    val type: String?,
    val revealed: Boolean,
)

data class GameViewDto(
    val currentTeam: String,
    val phase: String,
    val clueWord: String?,
    val clueCount: Int?,
    val guessesRemaining: Int,
    val cards: List<CardView>,
    val winner: String?,
    val redRemaining: Int,
    val blueRemaining: Int,
)
