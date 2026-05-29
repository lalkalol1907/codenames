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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
        GamesTable.update({ GamesTable.id eq game.id }) {
            it[currentTeam] = game.currentTeam.name
            it[phase] = game.phase.name
            it[clueWord] = game.clue?.word
            it[clueCount] = game.clue?.count
            it[guessesRemaining] = game.guessesRemaining
            it[winnerTeam] = game.winner?.name
        }
        game.cards.forEach { card ->
            CardsTable.update({ CardsTable.id eq card.id }) {
                it[revealed] = card.revealed
            }
        }
        game
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
        )
    }
}
