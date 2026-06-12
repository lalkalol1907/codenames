package com.lalkalol.room.dto

import com.lalkalol.common.model.Role
import com.lalkalol.common.model.RoomStatus
import com.lalkalol.common.model.Team
import com.lalkalol.game.dto.CardView
import com.lalkalol.game.dto.GameViewDto
import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.GamePhase
import com.lalkalol.game.model.GameState
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.service.RoomTeamRules
import java.util.UUID

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

        val activePlayers = RoomTeamRules.activePlayers(room.players)
        val canStart = room.status == RoomStatus.LOBBY &&
            room.hostPlayerId == viewerId &&
            RoomTeamRules.isReadyToStart(room.players)

        val canRandomizeTeams = room.status == RoomStatus.LOBBY &&
            room.hostPlayerId == viewerId &&
            RoomTeamRules.canRandomize(activePlayers.size)

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
        avatarUrl = avatarUrl,
    )
}
