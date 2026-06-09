package com.lalkalol.room.repository

import com.lalkalol.db.entity.PlayerEntity
import com.lalkalol.db.entity.RoomEntity
import com.lalkalol.db.jpa.CardJpaRepository
import com.lalkalol.db.jpa.GameJpaRepository
import com.lalkalol.db.jpa.PlayerJpaRepository
import com.lalkalol.db.jpa.RoomJpaRepository
import com.lalkalol.game.model.GameState
import com.lalkalol.common.model.Language
import com.lalkalol.common.model.Role
import com.lalkalol.common.model.RoomStatus
import com.lalkalol.common.model.Team
import com.lalkalol.game.repository.GameRepository
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.service.RoomException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RoomRepository(
    private val roomJpa: RoomJpaRepository,
    private val playerJpa: PlayerJpaRepository,
    private val gameJpa: GameJpaRepository,
    private val cardJpa: CardJpaRepository,
    private val gameRepository: GameRepository,
) {
    fun createRoom(code: String, language: Language, host: Player): Room {
        roomJpa.save(
            RoomEntity(
                id = host.roomId,
                code = code,
                language = language.code,
                hostPlayerId = host.id,
                status = RoomStatus.LOBBY.name,
            ),
        )
        playerJpa.save(
            PlayerEntity(
                id = host.id,
                roomId = host.roomId,
                name = host.name,
                team = null,
                role = null,
                isHost = true,
            ),
        )
        return requireNotNull(loadRoom(host.roomId))
    }

    fun findByCode(code: String): Room? {
        val roomRow = roomJpa.findByCode(code.uppercase()) ?: return null
        return loadRoom(roomRow.id)
    }

    fun addPlayer(player: Player): Room {
        playerJpa.save(
            PlayerEntity(
                id = player.id,
                roomId = player.roomId,
                name = player.name,
                team = null,
                role = null,
                isHost = false,
            ),
        )
        return requireNotNull(loadRoom(player.roomId))
    }

    fun updatePlayerRole(playerId: UUID, team: Team, role: Role): Room {
        val entity = playerJpa.findById(playerId).orElseThrow { RoomException("Player not in room") }
        entity.team = team.name
        entity.role = role.name
        playerJpa.save(entity)
        return requireNotNull(loadRoom(entity.roomId))
    }

    fun assignRoles(roomId: UUID, assignments: Map<UUID, Pair<Team, Role>>): Room {
        assignments.forEach { (playerId, teamRole) ->
            val entity = playerJpa.findById(playerId).orElseThrow { RoomException("Player not in room") }
            entity.team = teamRole.first.name
            entity.role = teamRole.second.name
            playerJpa.save(entity)
        }
        return requireNotNull(loadRoom(roomId))
    }

    fun updateStatus(roomId: UUID, status: RoomStatus) {
        val entity = roomJpa.findById(roomId).orElseThrow { RoomException("Room not found") }
        entity.status = status.name
        roomJpa.save(entity)
    }

    fun startGameAtomically(
        code: String,
        hostPlayerId: UUID,
        game: GameState,
        validate: (Room) -> Unit,
    ): Room {
        val room = loadByCodeForUpdate(code) ?: throw RoomException("Room not found")
        if (room.hostPlayerId != hostPlayerId) {
            throw RoomException("Only host can start the game")
        }
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Game already started")
        }
        validate(room)
        gameRepository.insertGameInTransaction(game)
        val entity = roomJpa.findById(room.id).orElseThrow { RoomException("Room not found") }
        entity.status = RoomStatus.PLAYING.name
        roomJpa.save(entity)
        return requireNotNull(loadRoom(room.id))
    }

    fun loadByCodeForUpdate(code: String): Room? {
        val roomRow = roomJpa.findByCodeForUpdate(code.uppercase()) ?: return null
        return loadRoom(roomRow.id)
    }

    fun codeExists(code: String): Boolean = roomJpa.existsByCode(code.uppercase())

    fun removePlayer(playerId: UUID) {
        playerJpa.deleteById(playerId)
    }

    fun transferHost(roomId: UUID, newHostPlayerId: UUID) {
        val room = roomJpa.findById(roomId).orElseThrow { RoomException("Room not found") }
        room.hostPlayerId = newHostPlayerId
        roomJpa.save(room)
        playerJpa.findAllByRoomId(roomId).forEach { player ->
            player.isHost = player.id == newHostPlayerId
            playerJpa.save(player)
        }
    }

    fun deleteRoom(roomId: UUID) {
        val game = gameJpa.findByRoomId(roomId)
        if (game != null) {
            cardJpa.deleteAllByGameId(game.id)
            gameJpa.deleteByRoomId(roomId)
        }
        playerJpa.deleteAllByRoomId(roomId)
        roomJpa.deleteById(roomId)
    }

    private fun loadRoom(roomId: UUID): Room? {
        val roomRow = roomJpa.findById(roomId).orElse(null) ?: return null
        val players = playerJpa.findAllByRoomId(roomId).map { row ->
            Player(
                id = row.id,
                roomId = row.roomId,
                name = row.name,
                team = row.team?.let { Team.valueOf(it) },
                role = row.role?.let { Role.valueOf(it) },
                isHost = row.isHost,
            )
        }
        val game = gameRepository.findByRoomIdInTransaction(roomId)
        return Room(
            id = roomRow.id,
            code = roomRow.code,
            language = Language.fromCode(roomRow.language),
            hostPlayerId = roomRow.hostPlayerId,
            status = RoomStatus.valueOf(roomRow.status),
            players = players,
            game = game,
        )
    }
}
