package com.lalkalol.game.service

import com.lalkalol.db.dbQuery
import com.lalkalol.db.tables.RoomsTable
import com.lalkalol.game.model.Clue
import com.lalkalol.game.model.GamePhase
import com.lalkalol.game.model.GameState
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import com.lalkalol.game.repository.GameRepository
import com.lalkalol.game.rules.BoardGenerator
import com.lalkalol.game.rules.TurnLogic
import com.lalkalol.game.rules.WinChecker
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.repository.RoomRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import java.util.UUID

class GameService(
    private val gameRepository: GameRepository,
    private val boardGenerator: BoardGenerator,
    private val roomRepository: RoomRepository,
) {
    suspend fun buildNewGame(room: Room): GameState {
        val gameId = UUID.randomUUID()
        val board = boardGenerator.generate(room.language, gameId)
        return GameState(
            id = gameId,
            roomId = room.id,
            startingTeam = board.startingTeam,
            currentTeam = board.startingTeam,
            phase = GamePhase.CLUE,
            clue = null,
            guessesRemaining = 0,
            cards = board.cards,
            winner = null,
        )
    }

    suspend fun startGame(room: Room): GameState {
        val game = buildNewGame(room)
        return gameRepository.createGame(game)
    }

    suspend fun giveClue(roomCode: String, playerId: UUID, word: String, count: Int): GameState = dbQuery {
        val (room, game, player) = lockedPlayingGame(roomCode, playerId)

        if (game.winner != null) throw GameException("Game is over")
        if (game.phase != GamePhase.CLUE) throw GameException("Not clue phase")
        if (player.role != Role.SPYMASTER) throw GameException("Only spymaster can give clues")
        if (player.team != game.currentTeam) throw GameException("Not your team's turn")

        val clueWord = word.trim()
        if (clueWord.isEmpty()) throw GameException("Clue word cannot be empty")
        if (count < 1) throw GameException("Clue count must be at least 1")
        if (game.cards.any { it.word.equals(clueWord, ignoreCase = true) }) {
            throw GameException("Clue cannot match a word on the board")
        }

        val updated = game.copy(
            phase = GamePhase.GUESSING,
            clue = Clue(clueWord, count),
            guessesRemaining = count + 1,
        )
        gameRepository.saveGameInTransaction(game, updated)
    }

    suspend fun guess(roomCode: String, playerId: UUID, position: Int): GameState = dbQuery {
        val (room, game, player) = lockedPlayingGame(roomCode, playerId)

        if (game.winner != null) throw GameException("Game is over")
        if (game.phase != GamePhase.GUESSING) throw GameException("Not guessing phase")
        if (player.role != Role.OPERATIVE) throw GameException("Only operatives can guess")
        if (player.team != game.currentTeam) throw GameException("Not your team's turn")
        if (position !in 0 until game.cards.size) throw GameException("Invalid card position")

        val card = game.cards[position]
        if (card.revealed) throw GameException("Card already revealed")

        val revealedCards = game.cards.map {
            if (it.position == position) it.copy(revealed = true) else it
        }
        val revealedCard = revealedCards.first { it.position == position }

        val winner = WinChecker.checkWinner(revealedCards, revealedCard.type, game.currentTeam)
        val updated = if (winner != null) {
            RoomsTable.update({ RoomsTable.id eq room.id }) {
                it[RoomsTable.status] = RoomStatus.FINISHED.name
            }
            game.copy(cards = revealedCards, winner = winner, phase = GamePhase.CLUE)
        } else {
            val guessesRemaining = game.guessesRemaining - 1
            val endTurn = TurnLogic.shouldEndTurnAfterGuess(revealedCard.type, game.currentTeam)
                || guessesRemaining <= 0

            if (endTurn) {
                game.copy(
                    cards = revealedCards,
                    currentTeam = TurnLogic.nextTeam(game.currentTeam),
                    phase = GamePhase.CLUE,
                    clue = null,
                    guessesRemaining = 0,
                )
            } else {
                game.copy(cards = revealedCards, guessesRemaining = guessesRemaining)
            }
        }

        gameRepository.saveGameInTransaction(game, updated)
    }

    suspend fun endTurn(roomCode: String, playerId: UUID): GameState = dbQuery {
        val (_, game, player) = lockedPlayingGame(roomCode, playerId)

        if (game.winner != null) throw GameException("Game is over")
        if (game.phase != GamePhase.GUESSING) throw GameException("Not guessing phase")
        if (player.role != Role.OPERATIVE) throw GameException("Only operatives can end turn")
        if (player.team != game.currentTeam) throw GameException("Not your team's turn")

        val updated = game.copy(
            currentTeam = TurnLogic.nextTeam(game.currentTeam),
            phase = GamePhase.CLUE,
            clue = null,
            guessesRemaining = 0,
        )
        gameRepository.saveGameInTransaction(game, updated)
    }

    private fun lockedPlayingGame(roomCode: String, playerId: UUID): Triple<Room, GameState, Player> {
        val room = roomRepository.loadByCodeForUpdate(roomCode.uppercase())
            ?: throw GameException("Room not found")
        if (room.status != RoomStatus.PLAYING || room.game == null) {
            throw GameException("Game is not in progress")
        }
        val game = room.game!!
        val player = room.players.find { it.id == playerId } ?: throw GameException("Player not found")
        return Triple(room, game, player)
    }
}

class GameException(message: String) : Exception(message)
