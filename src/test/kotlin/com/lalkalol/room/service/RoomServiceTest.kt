package com.lalkalol.room.service

import com.lalkalol.common.model.Language
import com.lalkalol.common.model.Role
import com.lalkalol.common.model.RoomStatus
import com.lalkalol.common.model.Team
import com.lalkalol.testsupport.SpringIntegrationTest
import com.lalkalol.testsupport.setupFourPlayerGame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoomServiceTest : SpringIntegrationTest() {
    @Test
    fun `createRoom assigns host`() {
        val (room, host) = roomService.createRoom(Language.RU, "Alice")

        assertEquals(RoomStatus.LOBBY, room.status)
        assertEquals(1, room.players.size)
        assertTrue(host.isHost)
        assertEquals(4, room.code.length)
    }

    @Test
    fun `joinRoom adds player to lobby`() {
        val (room, _) = roomService.createRoom(Language.EN, "Host")
        val (updated, guest) = roomService.joinRoom(room.code, "Guest")

        assertEquals(2, updated.players.size)
        assertEquals("Guest", guest.name)
        assertEquals(RoomStatus.LOBBY, updated.status)
    }

    @Test
    fun `duplicate spymaster assignment is rejected`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val (_, guest) = roomService.joinRoom(room.code, "Guest")

        roomService.setRole(room.code, host.id, Team.RED, Role.SPYMASTER)
        assertThrows<RoomException> {
            roomService.setRole(room.code, guest.id, Team.RED, Role.SPYMASTER)
        }
    }

    @Test
    fun `multiple operatives per team are allowed`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val (_, redOp2) = roomService.joinRoom(room.code, "RedOp2")
        val (_, redOp3) = roomService.joinRoom(room.code, "RedOp3")
        val (_, blueSpy) = roomService.joinRoom(room.code, "BlueSpy")
        val (_, blueOp) = roomService.joinRoom(room.code, "BlueOp")

        roomService.setRole(room.code, host.id, Team.RED, Role.SPYMASTER)
        roomService.setRole(room.code, redOp2.id, Team.RED, Role.OPERATIVE)
        roomService.setRole(room.code, redOp3.id, Team.RED, Role.OPERATIVE)
        roomService.setRole(room.code, blueSpy.id, Team.BLUE, Role.SPYMASTER)
        roomService.setRole(room.code, blueOp.id, Team.BLUE, Role.OPERATIVE)

        val updated = roomService.getRoom(room.code)!!
        assertEquals(2, updated.players.count { it.team == Team.RED && it.role == Role.OPERATIVE })
    }

    @Test
    fun `operative limit per team is enforced`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val players = (1..7).map { roomService.joinRoom(room.code, "P$it").second }

        roomService.setRole(room.code, host.id, Team.RED, Role.SPYMASTER)
        roomService.setRole(room.code, players[0].id, Team.RED, Role.OPERATIVE)
        roomService.setRole(room.code, players[1].id, Team.RED, Role.OPERATIVE)
        roomService.setRole(room.code, players[2].id, Team.RED, Role.OPERATIVE)

        assertThrows<RoomException> {
            roomService.setRole(room.code, players[3].id, Team.RED, Role.OPERATIVE)
        }
    }

    @Test
    fun `startGame allows uneven operative counts`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val players = (1..5).map { roomService.joinRoom(room.code, "P$it").second }

        roomService.setRole(room.code, host.id, Team.RED, Role.SPYMASTER)
        roomService.setRole(room.code, players[0].id, Team.RED, Role.OPERATIVE)
        roomService.setRole(room.code, players[1].id, Team.RED, Role.OPERATIVE)
        roomService.setRole(room.code, players[2].id, Team.RED, Role.OPERATIVE)
        roomService.setRole(room.code, players[3].id, Team.BLUE, Role.SPYMASTER)
        roomService.setRole(room.code, players[4].id, Team.BLUE, Role.OPERATIVE)

        val started = roomService.startGame(room.code, host.id)
        assertEquals(RoomStatus.PLAYING, started.status)
        assertEquals(3, started.players.count { it.team == Team.RED && it.role == Role.OPERATIVE })
        assertEquals(1, started.players.count { it.team == Team.BLUE && it.role == Role.OPERATIVE })
    }

    @Test
    fun `startGame requires four players with valid roles`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")

        assertThrows<RoomException> {
            roomService.startGame(room.code, host.id)
        }

        val setup = roomService.setupFourPlayerGame()
        assertEquals(RoomStatus.PLAYING, setup.room.status)
        assertNotNull(setup.room.game)
        assertEquals(25, setup.room.game!!.cards.size)
    }

    @Test
    fun `startGame is rejected when game already started`() {
        val setup = roomService.setupFourPlayerGame()

        assertThrows<RoomException> {
            roomService.startGame(setup.roomCode, setup.host.id)
        }
    }

    @Test
    fun `randomizeTeams assigns valid roles for four players`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        roomService.joinRoom(room.code, "P2")
        roomService.joinRoom(room.code, "P3")
        roomService.joinRoom(room.code, "P4")

        val randomized = roomService.randomizeTeams(room.code, host.id)

        assertEquals(4, randomized.players.size)
        assertEquals(1, randomized.players.count { it.team == Team.RED && it.role == Role.SPYMASTER })
        assertEquals(1, randomized.players.count { it.team == Team.BLUE && it.role == Role.SPYMASTER })
        assertTrue(RoomTeamRules.isReadyToStart(randomized.players))
    }

    @Test
    fun `randomizeTeams assigns valid roles for six players`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        (2..6).forEach { roomService.joinRoom(room.code, "P$it") }

        val randomized = roomService.randomizeTeams(room.code, host.id)

        assertEquals(6, randomized.players.size)
        assertTrue(RoomTeamRules.isReadyToStart(randomized.players))
    }

    @Test
    fun `leaveRoom deletes empty room`() {
        val (room, host) = roomService.createRoom(Language.RU, "Solo")

        val afterLeave = roomService.leaveRoom(room.code, host.id)

        assertNull(afterLeave)
        assertNull(roomService.getRoom(room.code))
    }

    @Test
    fun `leaveRoom transfers host`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val (_, guest) = roomService.joinRoom(room.code, "Guest")

        roomService.leaveRoom(room.code, host.id)
        val updated = roomService.getRoom(room.code)

        assertNotNull(updated)
        assertEquals(guest.id, updated!!.hostPlayerId)
        assertTrue(updated.players.single().isHost)
    }
}
