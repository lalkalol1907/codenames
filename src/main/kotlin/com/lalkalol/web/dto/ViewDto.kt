package com.lalkalol.web.dto

import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.GamePhase
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import com.lalkalol.game.model.GameState
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CardView(
    val position: Int,
    val word: String,
    val type: String?,
    val revealed: Boolean,
)

@Serializable
data class PlayerViewDto(
    val id: String,
    val name: String,
    val team: String?,
    val role: String?,
    val isHost: Boolean,
)

@Serializable
data class GameViewDto(
    val currentTeam: String,
    val phase: String,
    val clueWord: String?,
    val clueCount: Int?,
    val guessesRemaining: Int,
    val cards: List<CardView>,
    val winner: String?,
    val redRemaining: Int,
    val blueRemaining: Int,
)

@Serializable
data class RoomViewDto(
    val status: String,
    val hostPlayerId: String,
    val players: List<PlayerViewDto>,
    val game: GameViewDto?,
    val viewerId: String,
    val canGiveClue: Boolean,
    val canGuess: Boolean,
    val canEndTurn: Boolean,
    val canStart: Boolean,
    val canRandomizeTeams: Boolean,
)

@Serializable
data class WsStateMessage(
    val type: String = "state",
    val view: RoomViewDto,
)

@Serializable
data class WsErrorMessage(
    val type: String = "error",
    val message: String,
)

object ViewBuilder {
    fun buildRoomView(room: Room, viewerId: UUID): RoomViewDto {
        val viewer = room.players.find { it.id == viewerId }
        val game = room.game
        val gameView = game?.let { buildGameView(it, viewer) }

        val canGiveClue = game != null &&
            room.status == RoomStatus.PLAYING &&
            game.winner == null &&
            game.phase == GamePhase.CLUE &&
            viewer?.role == Role.SPYMASTER &&
            viewer.team == game.currentTeam

        val canGuess = game != null &&
            room.status == RoomStatus.PLAYING &&
            game.winner == null &&
            game.phase == GamePhase.GUESSING &&
            viewer?.role == Role.OPERATIVE &&
            viewer.team == game.currentTeam

        val canEndTurn = canGuess && game.guessesRemaining > 0

        val canStart = room.status == RoomStatus.LOBBY &&
            room.hostPlayerId == viewerId &&
            room.players.size >= 4 &&
            room.players.all { it.team != null && it.role != null }

        val canRandomizeTeams = room.status == RoomStatus.LOBBY &&
            room.hostPlayerId == viewerId &&
            room.players.size == 4

        return RoomViewDto(
            status = room.status.name,
            hostPlayerId = room.hostPlayerId.toString(),
            players = room.players.map { it.toDto() },
            game = gameView,
            viewerId = viewerId.toString(),
            canGiveClue = canGiveClue,
            canGuess = canGuess,
            canEndTurn = canEndTurn,
            canStart = canStart,
            canRandomizeTeams = canRandomizeTeams,
        )
    }

    private fun buildGameView(game: GameState, viewer: Player?): GameViewDto {
        val showColors = viewer?.role == Role.SPYMASTER
        return GameViewDto(
            currentTeam = game.currentTeam.name,
            phase = game.phase.name,
            clueWord = game.clue?.word,
            clueCount = game.clue?.count,
            guessesRemaining = game.guessesRemaining,
            cards = game.cards.map { card ->
                CardView(
                    position = card.position,
                    word = card.word,
                    type = when {
                        card.revealed -> card.type.name
                        showColors -> card.type.name
                        else -> null
                    },
                    revealed = card.revealed,
                )
            },
            winner = game.winner?.name,
            redRemaining = game.cards.count { it.type == CardType.RED && !it.revealed },
            blueRemaining = game.cards.count { it.type == CardType.BLUE && !it.revealed },
        )
    }

    private fun Player.toDto() = PlayerViewDto(
        id = id.toString(),
        name = name,
        team = team?.name,
        role = role?.name,
        isHost = isHost,
    )
}
