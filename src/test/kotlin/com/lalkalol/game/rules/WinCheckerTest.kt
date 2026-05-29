package com.lalkalol.game.rules

import com.lalkalol.game.model.Card
import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.Team
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WinCheckerTest {

    @Test
    fun `assassin reveal gives win to other team`() {
        val cards = fullBoard().map { it.copy(revealed = it.type != CardType.ASSASSIN) }
        assertEquals(Team.BLUE, WinChecker.checkWinner(cards, CardType.ASSASSIN, Team.RED))
    }

    @Test
    fun `all red cards revealed wins red`() {
        val cards = fullBoard().map { card ->
            if (card.type == CardType.RED) card.copy(revealed = true) else card
        }
        assertEquals(Team.RED, WinChecker.checkWinner(cards, CardType.RED, Team.RED))
    }

    @Test
    fun `all blue cards revealed wins blue`() {
        val cards = fullBoard().map { card ->
            if (card.type == CardType.BLUE) card.copy(revealed = true) else card
        }
        assertEquals(Team.BLUE, WinChecker.checkWinner(cards, CardType.BLUE, Team.BLUE))
    }

    @Test
    fun `no winner while team cards remain`() {
        val cards = fullBoard().mapIndexed { index, card -> card.copy(revealed = index < 5) }
        assertNull(WinChecker.checkWinner(cards, CardType.NEUTRAL, Team.RED))
    }

    private fun fullBoard(): List<Card> {
        val types = buildList {
            repeat(9) { add(CardType.RED) }
            repeat(8) { add(CardType.BLUE) }
            repeat(7) { add(CardType.NEUTRAL) }
            add(CardType.ASSASSIN)
        }
        return types.mapIndexed { index, type ->
            Card(
                id = UUID.randomUUID(),
                position = index,
                word = "w$index",
                type = type,
                revealed = false,
            )
        }
    }
}
