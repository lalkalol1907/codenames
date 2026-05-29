package com.lalkalol.game.rules

import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Team
import com.lalkalol.testsupport.withTestApp
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardGeneratorTest {

    @Test
    fun `generates 25 cards with valid codenames distribution`() = withTestApp {
        val generator = dependencies.resolve<BoardGenerator>()
        val board = generator.generate(Language.EN, UUID.randomUUID())

        assertEquals(25, board.cards.size)
        assertEquals(25, board.cards.map { it.position }.toSet().size)
        assertEquals(25, board.cards.map { it.word }.toSet().size)
        assertTrue(board.cards.none { it.revealed })

        val red = board.cards.count { it.type == CardType.RED }
        val blue = board.cards.count { it.type == CardType.BLUE }
        assertTrue(red == 9 && blue == 8 || red == 8 && blue == 9)
        assertEquals(7, board.cards.count { it.type == CardType.NEUTRAL })
        assertEquals(1, board.cards.count { it.type == CardType.ASSASSIN })

        val startingType = when (board.startingTeam) {
            Team.RED -> CardType.RED
            Team.BLUE -> CardType.BLUE
        }
        assertEquals(9, board.cards.count { it.type == startingType })
    }
}
