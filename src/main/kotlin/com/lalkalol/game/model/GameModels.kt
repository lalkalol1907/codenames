package com.lalkalol.game.model

import com.lalkalol.common.model.Team
import java.util.UUID

data class Card(
    val id: UUID,
    val position: Int,
    val word: String,
    val type: CardType,
    val revealed: Boolean,
)

data class Clue(
    val word: String,
    val count: Int,
)

data class GameState(
    val id: UUID,
    val roomId: UUID,
    val startingTeam: Team,
    val currentTeam: Team,
    val phase: GamePhase,
    val clue: Clue?,
    val guessesRemaining: Int,
    val cards: List<Card>,
    val winner: Team?,
    val version: Int = 0,
)
