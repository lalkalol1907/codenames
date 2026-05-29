package com.lalkalol.game.service

import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.GamePhase
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import com.lalkalol.game.rules.TurnLogic
import com.lalkalol.room.service.RoomException
import com.lalkalol.testsupport.gameService
import com.lalkalol.testsupport.requireGame
import com.lalkalol.testsupport.roomService
import com.lalkalol.testsupport.setupFourPlayerGame
import com.lalkalol.testsupport.withTestApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameServiceTest {

    @Test
    fun `giveClue moves game to guessing phase`() = withTestApp {
        val roomService = roomService()
        val gameService = gameService()
        val setup = roomService.setupFourPlayerGame()

        val game = gameService.giveClue(setup.roomCode, setup.currentSpymaster().id, "hint", 2)

        assertEquals(GamePhase.GUESSING, game.phase)
        assertEquals("hint", game.clue?.word)
        assertEquals(2, game.clue?.count)
        assertEquals(3, game.guessesRemaining)
    }

    @Test
    fun `giveClue rejects board word`() = withTestApp {
        val roomService = roomService()
        val gameService = gameService()
        val setup = roomService.setupFourPlayerGame()
        val boardWord = setup.room.requireGame().cards.first().word

        assertFailsWith<GameException> {
            gameService.giveClue(setup.roomCode, setup.currentSpymaster().id, boardWord, 1)
        }
    }

    @Test
    fun `guess reveals own team card and continues turn`() = withTestApp {
        val roomService = roomService()
        val gameService = gameService()
        val setup = roomService.setupFourPlayerGame()
        val team = setup.startingTeam()
        gameService.giveClue(setup.roomCode, setup.spymaster(team).id, "go", 1)

        val roomBefore = roomService.getRoom(setup.roomCode)!!.requireGame()
        val ownType = if (team == Team.RED) CardType.RED else CardType.BLUE
        val ownCard = roomBefore.cards.first { it.type == ownType && !it.revealed }

        val after = gameService.guess(setup.roomCode, setup.operative(team).id, ownCard.position)

        assertTrue(after.cards.first { it.position == ownCard.position }.revealed)
        assertEquals(GamePhase.GUESSING, after.phase)
        assertEquals(team, after.currentTeam)
    }

    @Test
    fun `guess wrong color ends turn`() = withTestApp {
        val roomService = roomService()
        val gameService = gameService()
        val setup = roomService.setupFourPlayerGame()
        val team = setup.startingTeam()
        gameService.giveClue(setup.roomCode, setup.spymaster(team).id, "go", 2)

        val roomBefore = roomService.getRoom(setup.roomCode)!!.requireGame()
        val wrongType = if (team == Team.RED) CardType.BLUE else CardType.RED
        val wrongCard = roomBefore.cards.first { it.type == wrongType && !it.revealed }

        val after = gameService.guess(setup.roomCode, setup.operative(team).id, wrongCard.position)

        assertEquals(GamePhase.CLUE, after.phase)
        assertEquals(TurnLogic.nextTeam(team), after.currentTeam)
        assertNull(after.clue)
    }

    @Test
    fun `assassin guess finishes game for opponent`() = withTestApp {
        val roomService = roomService()
        val gameService = gameService()
        val setup = roomService.setupFourPlayerGame()
        val team = setup.startingTeam()
        gameService.giveClue(setup.roomCode, setup.spymaster(team).id, "go", 3)

        val assassin = setup.room.requireGame().cards.first { it.type == CardType.ASSASSIN && !it.revealed }
        val after = gameService.guess(setup.roomCode, setup.operative(team).id, assassin.position)

        assertEquals(TurnLogic.nextTeam(team), after.winner)
        assertEquals(RoomStatus.FINISHED, roomService.getRoom(setup.roomCode)!!.status)
    }

    @Test
    fun `operative cannot give clue`() = withTestApp {
        val roomService = roomService()
        val gameService = gameService()
        val setup = roomService.setupFourPlayerGame()

        assertFailsWith<GameException> {
            gameService.giveClue(setup.roomCode, setup.redOperative.id, "nope", 1)
        }
    }
}
