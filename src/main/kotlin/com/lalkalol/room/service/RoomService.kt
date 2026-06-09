package com.lalkalol.room.service

import com.lalkalol.common.model.Language
import com.lalkalol.common.model.Role
import com.lalkalol.common.model.RoomStatus
import com.lalkalol.common.model.Team
import com.lalkalol.game.service.GameService
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.repository.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.random.Random

@Service
class RoomService(
    private val roomRepository: RoomRepository,
    private val gameService: GameService,
) {
    @Transactional
    fun createRoom(language: Language, hostName: String): Pair<Room, Player> {
        val roomId = UUID.randomUUID()
        val hostId = UUID.randomUUID()
        val host = Player(
            id = hostId,
            roomId = roomId,
            name = hostName.trim(),
            team = null,
            role = null,
            isHost = true,
        )
        val code = generateUniqueCode()
        val room = roomRepository.createRoom(code, language, host)
        return room to host
    }

    @Transactional
    fun joinRoom(code: String, playerName: String): Pair<Room, Player> {
        val room = roomRepository.loadByCodeForUpdate(code.uppercase())
            ?: throw RoomException("Room not found")
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Game already started")
        }
        val player = Player(
            id = UUID.randomUUID(),
            roomId = room.id,
            name = playerName.trim(),
            team = null,
            role = null,
            isHost = false,
        )
        val updated = roomRepository.addPlayer(player)
        return updated to player
    }

    @Transactional
    fun setRole(roomCode: String, playerId: UUID, team: Team?, role: Role): Room {
        val room = requireRoomForUpdate(roomCode)
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Cannot change role after game started")
        }
        val effectiveTeam = if (role == Role.SPECTATOR) null else team
        validateRoleAssignment(room, playerId, effectiveTeam, role)
        return roomRepository.updatePlayerRole(playerId, effectiveTeam, role)
    }

    @Transactional
    fun randomizeTeams(roomCode: String, hostPlayerId: UUID): Room {
        val room = requireRoomForUpdate(roomCode)
        if (room.hostPlayerId != hostPlayerId) {
            throw RoomException("Only host can randomize teams")
        }
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Cannot change role after game started")
        }
        val activePlayers = room.players.filter { it.role != Role.SPECTATOR }
        if (activePlayers.size != 4) {
            throw RoomException("Exactly 4 players required for random teams")
        }
        val slots = listOf(
            Team.RED to Role.SPYMASTER,
            Team.RED to Role.OPERATIVE,
            Team.BLUE to Role.SPYMASTER,
            Team.BLUE to Role.OPERATIVE,
        )
        val assignments = activePlayers.shuffled(Random.Default).zip(slots).associate { (player, slot) ->
            player.id to slot
        }
        return roomRepository.assignRoles(room.id, assignments)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun startGame(roomCode: String, hostPlayerId: UUID): Room {
        val room = requireRoom(roomCode)
        if (room.hostPlayerId != hostPlayerId) {
            throw RoomException("Only host can start the game")
        }
        validateReadyToStart(room)
        val game = gameService.buildNewGame(room)
        return roomRepository.startGameAtomically(room.code, hostPlayerId, game) { lockedRoom ->
            validateReadyToStart(lockedRoom)
        }
    }

    fun getRoom(code: String): Room? = roomRepository.findByCode(code.uppercase())

    @Transactional
    fun leaveRoom(roomCode: String, playerId: UUID): Room? {
        val room = roomRepository.findByCode(roomCode.uppercase()) ?: return null
        if (room.status != RoomStatus.LOBBY) {
            return room
        }
        if (room.players.none { it.id == playerId }) {
            return room
        }
        val remaining = room.players.filter { it.id != playerId }
        if (remaining.isEmpty()) {
            roomRepository.deleteRoom(room.id)
            return null
        }
        if (room.hostPlayerId == playerId) {
            roomRepository.transferHost(room.id, remaining.first().id)
        }
        roomRepository.removePlayer(playerId)
        return roomRepository.findByCode(roomCode.uppercase())
    }

    private fun requireRoom(code: String): Room =
        roomRepository.findByCode(code.uppercase()) ?: throw RoomException("Room not found")

    private fun requireRoomForUpdate(code: String): Room =
        roomRepository.loadByCodeForUpdate(code.uppercase()) ?: throw RoomException("Room not found")

    private fun validateRoleAssignment(room: Room, playerId: UUID, team: Team?, role: Role) {
        if (room.players.none { it.id == playerId }) {
            throw RoomException("Player not in room")
        }
        if (role == Role.SPECTATOR) {
            return
        }
        if (team == null) {
            throw RoomException("Invalid team or role")
        }
        val others = room.players.filter { it.id != playerId }
        if (others.any { it.team == team && it.role == role }) {
            throw RoomException("Role already taken")
        }
    }

    private fun validateReadyToStart(room: Room) {
        val activePlayers = room.players.filter { it.role != Role.SPECTATOR }
        if (activePlayers.size < 4) {
            throw RoomException("At least 4 players required")
        }
        val redSpymaster = activePlayers.count { it.team == Team.RED && it.role == Role.SPYMASTER }
        val redOperative = activePlayers.count { it.team == Team.RED && it.role == Role.OPERATIVE }
        val blueSpymaster = activePlayers.count { it.team == Team.BLUE && it.role == Role.SPYMASTER }
        val blueOperative = activePlayers.count { it.team == Team.BLUE && it.role == Role.OPERATIVE }
        if (redSpymaster != 1 || redOperative != 1 || blueSpymaster != 1 || blueOperative != 1) {
            throw RoomException("Each team needs exactly 1 spymaster and 1 operative")
        }
        if (activePlayers.any { it.team == null || it.role == null }) {
            throw RoomException("All players must choose team and role")
        }
    }

    private fun generateUniqueCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(100) {
            val code = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
            if (!roomRepository.codeExists(code)) {
                return code
            }
        }
        throw RoomException("Could not generate room code")
    }
}

class RoomException(message: String) : Exception(message)
