package com.lalkalol.game.rules

import com.lalkalol.common.model.Team
import com.lalkalol.game.model.CardType

object TurnLogic {
    fun shouldEndTurnAfterGuess(cardType: CardType, currentTeam: Team): Boolean {
        val teamType = when (currentTeam) {
            Team.RED -> CardType.RED
            Team.BLUE -> CardType.BLUE
        }
        return cardType != teamType
    }

    fun nextTeam(current: Team): Team = when (current) {
        Team.RED -> Team.BLUE
        Team.BLUE -> Team.RED
    }
}
