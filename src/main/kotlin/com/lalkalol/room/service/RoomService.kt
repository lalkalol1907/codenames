package com.lalkalol.room.service

import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import com.lalkalol.game.service.GameService
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.repository.RoomRepository
import java.util.UUID
import kotlin.random.Random

class RoomService(
    private val roomRepository: RoomRepository,
    private val gameService: GameService,
) {
    suspend fun createRoom(language: Language, hostName: String): Pair<Room, Player> {
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

    suspend fun joinRoom(code: String, playerName: String): Pair<Room, Player> {
        val room = roomRepository.findByCode(code.uppercase())
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

    suspend fun setRole(roomCode: String, playerId: UUID, team: Team, role: Role): Room {
        val room = requireRoom(roomCode)
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Cannot change role after game started")
        }
        validateRoleAssignment(room, playerId, team, role)
        return roomRepository.updatePlayerRole(playerId, team, role)
    }

    suspend fun randomizeTeams(roomCode: String, hostPlayerId: UUID): Room {
        val room = requireRoom(roomCode)
        if (room.hostPlayerId != hostPlayerId) {
            throw RoomException("Only host can randomize teams")
        }
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Cannot change role after game started")
        }
        if (room.players.size != 4) {
            throw RoomException("Exactly 4 players required for random teams")
        }
        val slots = listOf(
            Team.RED to Role.SPYMASTER,
            Team.RED to Role.OPERATIVE,
            Team.BLUE to Role.SPYMASTER,
            Team.BLUE to Role.OPERATIVE,
        )
        val assignments = room.players.shuffled(Random.Default).zip(slots).associate { (player, slot) ->
            player.id to slot
        }
        return roomRepository.assignRoles(room.id, assignments)
    }

    suspend fun startGame(roomCode: String, hostPlayerId: UUID): Room {
        val room = requireRoom(roomCode)
        if (room.hostPlayerId != hostPlayerId) {
            throw RoomException("Only host can start the game")
        }
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Game already started")
        }
        validateReadyToStart(room)
        gameService.startGame(room)
        roomRepository.updateStatus(room.id, RoomStatus.PLAYING)
        return requireNotNull(roomRepository.findByCode(roomCode))
    }

    suspend fun getRoom(code: String): Room? = roomRepository.findByCode(code.uppercase())

    suspend fun leaveRoom(roomCode: String, playerId: UUID): Room? {
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

    private suspend fun requireRoom(code: String): Room =
        roomRepository.findByCode(code.uppercase()) ?: throw RoomException("Room not found")

    private fun validateRoleAssignment(room: Room, playerId: UUID, team: Team, role: Role) {
        if (room.players.none { it.id == playerId }) {
            throw RoomException("Player not in room")
        }
        val others = room.players.filter { it.id != playerId }
        if (others.any { it.team == team && it.role == role }) {
            throw RoomException("Role already taken")
        }
    }

    private fun validateReadyToStart(room: Room) {
        if (room.players.size < 4) {
            throw RoomException("At least 4 players required")
        }
        val redSpymaster = room.players.count { it.team == Team.RED && it.role == Role.SPYMASTER }
        val redOperative = room.players.count { it.team == Team.RED && it.role == Role.OPERATIVE }
        val blueSpymaster = room.players.count { it.team == Team.BLUE && it.role == Role.SPYMASTER }
        val blueOperative = room.players.count { it.team == Team.BLUE && it.role == Role.OPERATIVE }
        if (redSpymaster != 1 || redOperative != 1 || blueSpymaster != 1 || blueOperative != 1) {
            throw RoomException("Each team needs exactly 1 spymaster and 1 operative")
        }
        if (room.players.any { it.team == null || it.role == null }) {
            throw RoomException("All players must choose team and role")
        }
    }

    private suspend fun generateUniqueCode(): String {
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
