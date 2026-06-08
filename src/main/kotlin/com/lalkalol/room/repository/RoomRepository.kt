package com.lalkalol.room.repository

import com.lalkalol.db.dbQuery
import com.lalkalol.db.tables.CardsTable
import com.lalkalol.db.tables.GamesTable
import com.lalkalol.db.tables.PlayersTable
import com.lalkalol.db.tables.RoomsTable
import com.lalkalol.game.model.GameState
import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import com.lalkalol.game.repository.GameRepository
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.service.RoomException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.vendors.ForUpdateOption
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class RoomRepository(
    private val gameRepository: GameRepository,
) {
    suspend fun createRoom(code: String, language: Language, host: Player): Room = dbQuery {
        RoomsTable.insert {
            it[id] = host.roomId
            it[RoomsTable.code] = code
            it[RoomsTable.language] = language.code
            it[hostPlayerId] = host.id
            it[status] = RoomStatus.LOBBY.name
        }
        PlayersTable.insert {
            it[id] = host.id
            it[roomId] = host.roomId
            it[name] = host.name
            it[team] = null
            it[role] = null
            it[isHost] = true
        }
        requireNotNull(loadRoom(host.roomId))
    }

    suspend fun findByCode(code: String): Room? = dbQuery {
        val roomRow = RoomsTable.selectAll().where { RoomsTable.code eq code.uppercase() }.singleOrNull()
            ?: return@dbQuery null
        loadRoom(roomRow[RoomsTable.id])
    }

    suspend fun addPlayer(player: Player): Room = dbQuery {
        PlayersTable.insert {
            it[id] = player.id
            it[roomId] = player.roomId
            it[name] = player.name
            it[team] = null
            it[role] = null
            it[isHost] = false
        }
        requireNotNull(loadRoom(player.roomId))
    }

    suspend fun updatePlayerRole(playerId: UUID, team: Team, role: Role): Room = dbQuery {
        PlayersTable.update({ PlayersTable.id eq playerId }) {
            it[PlayersTable.team] = team.name
            it[PlayersTable.role] = role.name
        }
        val roomId = PlayersTable.selectAll().where { PlayersTable.id eq playerId }
            .single()[PlayersTable.roomId]
        requireNotNull(loadRoom(roomId))
    }

    suspend fun assignRoles(roomId: UUID, assignments: Map<UUID, Pair<Team, Role>>): Room = dbQuery {
        assignments.forEach { (playerId, teamRole) ->
            PlayersTable.update({ PlayersTable.id eq playerId }) {
                it[PlayersTable.team] = teamRole.first.name
                it[PlayersTable.role] = teamRole.second.name
            }
        }
        requireNotNull(loadRoom(roomId))
    }

    suspend fun updateStatus(roomId: UUID, status: RoomStatus): Unit = dbQuery {
        RoomsTable.update({ RoomsTable.id eq roomId }) {
            it[RoomsTable.status] = status.name
        }
    }

    suspend fun startGameAtomically(
        code: String,
        hostPlayerId: UUID,
        game: GameState,
        validate: (Room) -> Unit,
    ): Room = dbQuery {
        val room = loadByCodeForUpdate(code) ?: throw RoomException("Room not found")
        if (room.hostPlayerId != hostPlayerId) {
            throw RoomException("Only host can start the game")
        }
        if (room.status != RoomStatus.LOBBY) {
            throw RoomException("Game already started")
        }
        validate(room)
        gameRepository.insertGameInTransaction(game)
        RoomsTable.update({ RoomsTable.id eq room.id }) {
            it[RoomsTable.status] = RoomStatus.PLAYING.name
        }
        requireNotNull(loadRoom(room.id))
    }

    fun loadByCodeForUpdate(code: String): Room? {
        val roomRow = RoomsTable.selectAll()
            .where { RoomsTable.code eq code.uppercase() }
            .forUpdate(ForUpdateOption.ForUpdate)
            .singleOrNull() ?: return null
        return loadRoom(roomRow[RoomsTable.id])
    }

    suspend fun codeExists(code: String): Boolean = dbQuery {
        RoomsTable.selectAll().where { RoomsTable.code eq code.uppercase() }.any()
    }

    suspend fun removePlayer(playerId: UUID): Unit = dbQuery {
        PlayersTable.deleteWhere { PlayersTable.id eq playerId }
    }

    suspend fun transferHost(roomId: UUID, newHostPlayerId: UUID): Unit = dbQuery {
        RoomsTable.update({ RoomsTable.id eq roomId }) {
            it[hostPlayerId] = newHostPlayerId
        }
        PlayersTable.update({ PlayersTable.roomId eq roomId }) {
            it[isHost] = false
        }
        PlayersTable.update({ PlayersTable.id eq newHostPlayerId }) {
            it[isHost] = true
        }
    }

    suspend fun deleteRoom(roomId: UUID): Unit = dbQuery {
        val gameRow = GamesTable.selectAll().where { GamesTable.roomId eq roomId }.singleOrNull()
        if (gameRow != null) {
            val gameId = gameRow[GamesTable.id]
            CardsTable.deleteWhere { CardsTable.gameId eq gameId }
            GamesTable.deleteWhere { GamesTable.id eq gameId }
        }
        PlayersTable.deleteWhere { PlayersTable.roomId eq roomId }
        RoomsTable.deleteWhere { RoomsTable.id eq roomId }
    }

    private fun loadRoom(roomId: UUID): Room? {
        val roomRow = RoomsTable.selectAll().where { RoomsTable.id eq roomId }.singleOrNull()
            ?: return null
        val players = PlayersTable.selectAll().where { PlayersTable.roomId eq roomId }.map { row ->
            Player(
                id = row[PlayersTable.id],
                roomId = row[PlayersTable.roomId],
                name = row[PlayersTable.name],
                team = row[PlayersTable.team]?.let { Team.valueOf(it) },
                role = row[PlayersTable.role]?.let { Role.valueOf(it) },
                isHost = row[PlayersTable.isHost],
            )
        }
        val game = gameRepository.findByRoomIdInTransaction(roomId)
        return Room(
            id = roomRow[RoomsTable.id],
            code = roomRow[RoomsTable.code],
            language = Language.fromCode(roomRow[RoomsTable.language]),
            hostPlayerId = roomRow[RoomsTable.hostPlayerId],
            status = RoomStatus.valueOf(roomRow[RoomsTable.status]),
            players = players,
            game = game,
        )
    }
}
