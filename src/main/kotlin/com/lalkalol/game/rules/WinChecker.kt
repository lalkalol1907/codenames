package com.lalkalol.game.rules

import com.lalkalol.common.model.Team
import com.lalkalol.game.model.Card
import com.lalkalol.game.model.CardType

object WinChecker {
    fun checkWinner(cards: List<Card>, lastRevealed: CardType, guessingTeam: Team): Team? {
        if (lastRevealed == CardType.ASSASSIN) {
            return TurnLogic.nextTeam(guessingTeam)
        }
        val redRemaining = cards.count { it.type == CardType.RED && !it.revealed }
        val blueRemaining = cards.count { it.type == CardType.BLUE && !it.revealed }
        return when {
            redRemaining == 0 -> Team.RED
            blueRemaining == 0 -> Team.BLUE
            else -> null
        }
    }
}
