package com.lalkalol.room.service

import com.lalkalol.common.model.Role
import com.lalkalol.common.model.Team
import com.lalkalol.room.model.Player
import java.util.UUID
import kotlin.random.Random

object RoomTeamRules {
    const val MAX_OPERATIVES_PER_TEAM = 3
    const val MIN_OPERATIVES_PER_TEAM = 1
    const val MIN_ACTIVE_PLAYERS = 4
    const val MAX_ACTIVE_PLAYERS = 8

    fun activePlayers(players: List<Player>): List<Player> =
        players.filter { it.role != Role.SPECTATOR }

    fun validateRoleAssignment(players: List<Player>, playerId: UUID, team: Team?, role: Role) {
        if (players.none { it.id == playerId }) {
            throw RoomException("Player not in room")
        }
        if (role == Role.SPECTATOR) {
            return
        }
        if (team == null) {
            throw RoomException("Invalid team or role")
        }
        val others = players.filter { it.id != playerId }
        when (role) {
            Role.SPYMASTER -> {
                if (others.any { it.team == team && it.role == Role.SPYMASTER }) {
                    throw RoomException("Role already taken")
                }
            }
            Role.OPERATIVE -> {
                val operativeCount = others.count { it.team == team && it.role == Role.OPERATIVE }
                if (operativeCount >= MAX_OPERATIVES_PER_TEAM) {
                    throw RoomException("Team already has maximum operatives")
                }
            }
            Role.SPECTATOR -> Unit
        }
    }

    fun validateReadyToStart(players: List<Player>) {
        val active = activePlayers(players)
        if (active.size < MIN_ACTIVE_PLAYERS) {
            throw RoomException("At least 4 players required")
        }
        if (active.any { it.team == null || it.role == null }) {
            throw RoomException("All players must choose team and role")
        }
        if (!isTeamCompositionValid(active)) {
            throw RoomException("Each team needs 1 spymaster and 1 to 3 operatives")
        }
    }

    fun isReadyToStart(players: List<Player>): Boolean {
        val active = activePlayers(players)
        if (active.size < MIN_ACTIVE_PLAYERS) return false
        if (active.any { it.team == null || it.role == null }) return false
        return isTeamCompositionValid(active)
    }

    fun canRandomize(activeCount: Int): Boolean =
        activeCount in MIN_ACTIVE_PLAYERS..MAX_ACTIVE_PLAYERS

    fun randomRoleAssignments(players: List<Player>): Map<UUID, Pair<Team, Role>> {
        require(players.size in MIN_ACTIVE_PLAYERS..MAX_ACTIVE_PLAYERS) {
            "Between 4 and 8 active players required for random teams"
        }

        val shuffled = players.shuffled(Random.Default)
        val assignments = mutableMapOf<UUID, Pair<Team, Role>>()

        assignments[shuffled[0].id] = Team.RED to Role.SPYMASTER
        assignments[shuffled[1].id] = Team.BLUE to Role.SPYMASTER

        val operativePlayers = shuffled.drop(2).shuffled(Random.Default)
        val redOperatives = mutableListOf(operativePlayers.first())
        val blueOperatives = mutableListOf(operativePlayers[1])

        for (player in operativePlayers.drop(2)) {
            val assignRed = when {
                redOperatives.size >= MAX_OPERATIVES_PER_TEAM -> false
                blueOperatives.size >= MAX_OPERATIVES_PER_TEAM -> true
                Random.Default.nextBoolean() -> true
                else -> false
            }
            if (assignRed) {
                redOperatives.add(player)
            } else {
                blueOperatives.add(player)
            }
        }

        redOperatives.forEach { assignments[it.id] = Team.RED to Role.OPERATIVE }
        blueOperatives.forEach { assignments[it.id] = Team.BLUE to Role.OPERATIVE }

        return assignments
    }

    private fun isTeamCompositionValid(active: List<Player>): Boolean =
        Team.entries.all { team ->
            val spymasters = active.count { it.team == team && it.role == Role.SPYMASTER }
            val operatives = active.count { it.team == team && it.role == Role.OPERATIVE }
            spymasters == 1 && operatives in MIN_OPERATIVES_PER_TEAM..MAX_OPERATIVES_PER_TEAM
        }
}
