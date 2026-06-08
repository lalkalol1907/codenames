package com.lalkalol.game.repository

import com.lalkalol.db.dbQuery
import com.lalkalol.db.tables.CardsTable
import com.lalkalol.db.tables.GamesTable
import com.lalkalol.game.model.Card
import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.Clue
import com.lalkalol.game.model.GamePhase
import com.lalkalol.game.model.GameState
import com.lalkalol.game.model.Team
import com.lalkalol.game.service.GameException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class GameRepository {
    suspend fun createGame(game: GameState): GameState = dbQuery {
        insertGame(game)
        game
    }

    suspend fun saveGame(game: GameState): GameState = dbQuery {
        saveGameInTransaction(game, game)
    }

    fun insertGameInTransaction(game: GameState) {
        insertGame(game)
    }

    fun saveGameInTransaction(expected: GameState, updated: GameState): GameState {
        require(expected.id == updated.id) { "Game id mismatch" }

        val gameRows = GamesTable.update({
            (GamesTable.id eq expected.id) and (GamesTable.version eq expected.version)
        }) {
            it[currentTeam] = updated.currentTeam.name
            it[phase] = updated.phase.name
            it[clueWord] = updated.clue?.word
            it[clueCount] = updated.clue?.count
            it[guessesRemaining] = updated.guessesRemaining
            it[winnerTeam] = updated.winner?.name
            it[version] = expected.version + 1
        }
        if (gameRows == 0) {
            throw GameException("Game state changed")
        }

        updated.cards.forEach { card ->
            val previous = expected.cards.first { it.id == card.id }
            if (card.revealed && !previous.revealed) {
                val cardRows = CardsTable.update({
                    (CardsTable.id eq card.id) and (CardsTable.revealed eq false)
                }) {
                    it[revealed] = true
                }
                if (cardRows == 0) {
                    throw GameException("Card already revealed")
                }
            }
        }

        return updated.copy(version = expected.version + 1)
    }

    fun findByRoomIdInTransaction(roomId: UUID): GameState? {
        val gameRow = GamesTable.selectAll().where { GamesTable.roomId eq roomId }.singleOrNull()
            ?: return null
        return loadGame(gameRow[GamesTable.id])
    }

    private fun insertGame(game: GameState) {
        GamesTable.insert {
            it[id] = game.id
            it[roomId] = game.roomId
            it[startingTeam] = game.startingTeam.name
            it[currentTeam] = game.currentTeam.name
            it[phase] = game.phase.name
            it[clueWord] = game.clue?.word
            it[clueCount] = game.clue?.count
            it[guessesRemaining] = game.guessesRemaining
            it[winnerTeam] = game.winner?.name
            it[version] = game.version
        }
        CardsTable.batchInsert(game.cards) { card ->
            this[CardsTable.id] = card.id
            this[CardsTable.gameId] = game.id
            this[CardsTable.position] = card.position
            this[CardsTable.word] = card.word
            this[CardsTable.cardType] = card.type.name
            this[CardsTable.revealed] = card.revealed
        }
    }

    private fun loadGame(gameId: UUID): GameState? {
        val gameRow = GamesTable.selectAll().where { GamesTable.id eq gameId }.singleOrNull()
            ?: return null
        val cards = CardsTable.selectAll()
            .where { CardsTable.gameId eq gameId }
            .orderBy(CardsTable.position)
            .map { row ->
                Card(
                    id = row[CardsTable.id],
                    position = row[CardsTable.position],
                    word = row[CardsTable.word],
                    type = CardType.valueOf(row[CardsTable.cardType]),
                    revealed = row[CardsTable.revealed],
                )
            }
        val clueWord = gameRow[GamesTable.clueWord]
        val clueCount = gameRow[GamesTable.clueCount]
        val clue = if (clueWord != null && clueCount != null) Clue(clueWord, clueCount) else null
        return GameState(
            id = gameRow[GamesTable.id],
            roomId = gameRow[GamesTable.roomId],
            startingTeam = Team.valueOf(gameRow[GamesTable.startingTeam]),
            currentTeam = Team.valueOf(gameRow[GamesTable.currentTeam]),
            phase = GamePhase.valueOf(gameRow[GamesTable.phase]),
            clue = clue,
            guessesRemaining = gameRow[GamesTable.guessesRemaining],
            cards = cards,
            winner = gameRow[GamesTable.winnerTeam]?.let { Team.valueOf(it) },
            version = gameRow[GamesTable.version],
        )
    }
}
