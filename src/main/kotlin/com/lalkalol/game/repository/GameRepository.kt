package com.lalkalol.game.repository

import com.lalkalol.db.entity.CardEntity
import com.lalkalol.db.entity.GameEntity
import com.lalkalol.db.jpa.CardJpaRepository
import com.lalkalol.db.jpa.GameJpaRepository
import com.lalkalol.game.model.Card
import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.Clue
import com.lalkalol.game.model.GamePhase
import com.lalkalol.game.model.GameState
import com.lalkalol.common.model.Team
import com.lalkalol.game.service.GameException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class GameRepository(
    private val gameJpa: GameJpaRepository,
    private val cardJpa: CardJpaRepository,
) {
    fun insertGameInTransaction(game: GameState) {
        insertGame(game)
    }

    fun saveGameInTransaction(expected: GameState, updated: GameState): GameState {
        require(expected.id == updated.id) { "Game id mismatch" }

        val entity = gameJpa.findById(expected.id).orElseThrow {
            GameException("Game state changed")
        }
        if (entity.version != expected.version) {
            throw GameException("Game state changed")
        }

        entity.currentTeam = updated.currentTeam.name
        entity.phase = updated.phase.name
        entity.clueWord = updated.clue?.word
        entity.clueCount = updated.clue?.count
        entity.guessesRemaining = updated.guessesRemaining
        entity.winnerTeam = updated.winner?.name

        updated.cards.forEach { card ->
            val previous = expected.cards.first { it.id == card.id }
            if (card.revealed && !previous.revealed) {
                val cardEntity = cardJpa.findById(card.id).orElseThrow {
                    GameException("Card already revealed")
                }
                if (cardEntity.revealed) {
                    throw GameException("Card already revealed")
                }
                cardEntity.revealed = true
                cardJpa.save(cardEntity)
            }
        }

        val saved = gameJpa.saveAndFlush(entity)
        return updated.copy(version = saved.version)
    }

    fun findByRoomIdInTransaction(roomId: UUID): GameState? {
        val gameRow = gameJpa.findByRoomId(roomId) ?: return null
        return loadGame(gameRow.id)
    }

    private fun insertGame(game: GameState) {
        gameJpa.save(
            GameEntity(
                id = game.id,
                roomId = game.roomId,
                startingTeam = game.startingTeam.name,
                currentTeam = game.currentTeam.name,
                phase = game.phase.name,
                clueWord = game.clue?.word,
                clueCount = game.clue?.count,
                guessesRemaining = game.guessesRemaining,
                winnerTeam = game.winner?.name,
                version = game.version,
            ),
        )
        cardJpa.saveAll(
            game.cards.map { card ->
                CardEntity(
                    id = card.id,
                    gameId = game.id,
                    position = card.position,
                    word = card.word,
                    cardType = card.type.name,
                    revealed = card.revealed,
                )
            },
        )
    }

    private fun loadGame(gameId: UUID): GameState? {
        val gameRow = gameJpa.findById(gameId).orElse(null) ?: return null
        val cards = cardJpa.findAllByGameIdOrderByPositionAsc(gameId).map { row ->
            Card(
                id = row.id,
                position = row.position,
                word = row.word,
                type = CardType.valueOf(row.cardType),
                revealed = row.revealed,
            )
        }
        val clueWord = gameRow.clueWord
        val clueCount = gameRow.clueCount
        val clue = if (clueWord != null && clueCount != null) Clue(clueWord, clueCount) else null
        return GameState(
            id = gameRow.id,
            roomId = gameRow.roomId,
            startingTeam = Team.valueOf(gameRow.startingTeam),
            currentTeam = Team.valueOf(gameRow.currentTeam),
            phase = GamePhase.valueOf(gameRow.phase),
            clue = clue,
            guessesRemaining = gameRow.guessesRemaining,
            cards = cards,
            winner = gameRow.winnerTeam?.let { Team.valueOf(it) },
            version = gameRow.version,
        )
    }
}
