package com.lalkalol.game.rules

import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TurnLogicTest {

    @Test
    fun `nextTeam alternates red and blue`() {
        assertEquals(Team.BLUE, TurnLogic.nextTeam(Team.RED))
        assertEquals(Team.RED, TurnLogic.nextTeam(Team.BLUE))
    }

    @Test
    fun `turn continues after own team card`() {
        assertFalse(TurnLogic.shouldEndTurnAfterGuess(CardType.RED, Team.RED))
        assertFalse(TurnLogic.shouldEndTurnAfterGuess(CardType.BLUE, Team.BLUE))
    }

    @Test
    fun `turn ends after neutral opponent or assassin card`() {
        assertTrue(TurnLogic.shouldEndTurnAfterGuess(CardType.NEUTRAL, Team.RED))
        assertTrue(TurnLogic.shouldEndTurnAfterGuess(CardType.BLUE, Team.RED))
        assertTrue(TurnLogic.shouldEndTurnAfterGuess(CardType.ASSASSIN, Team.BLUE))
    }
}
