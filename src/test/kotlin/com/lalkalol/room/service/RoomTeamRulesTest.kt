package com.lalkalol.room.service

import com.lalkalol.common.model.Role
import com.lalkalol.room.model.Player
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import java.util.UUID

class RoomTeamRulesTest {
    @RepeatedTest(20)
    fun `randomRoleAssignments always produces a valid composition`() {
        for (count in 4..8) {
            val players = (1..count).map { index ->
                Player(
                    id = UUID.randomUUID(),
                    roomId = UUID.randomUUID(),
                    name = "P$index",
                    team = null,
                    role = null,
                    isHost = index == 1,
                )
            }

            val assignments = RoomTeamRules.randomRoleAssignments(players)
            val updated = players.map { player ->
                val assignment = assignments.getValue(player.id)
                player.copy(team = assignment.first, role = assignment.second)
            }

            assertTrue(RoomTeamRules.isReadyToStart(updated))
            assertTrue(updated.all { it.role != Role.SPECTATOR })
        }
    }
}
