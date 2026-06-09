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
    fun `duplicate role assignment is rejected`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val (_, guest) = roomService.joinRoom(room.code, "Guest")

        roomService.setRole(room.code, host.id, Team.RED, Role.SPYMASTER)
        assertThrows<RoomException> {
            roomService.setRole(room.code, guest.id, Team.RED, Role.SPYMASTER)
        }
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
    fun `randomizeTeams assigns all roles for four players`() {
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        roomService.joinRoom(room.code, "P2")
        roomService.joinRoom(room.code, "P3")
        roomService.joinRoom(room.code, "P4")

        val randomized = roomService.randomizeTeams(room.code, host.id)

        assertEquals(4, randomized.players.size)
        assertEquals(1, randomized.players.count { it.team == Team.RED && it.role == Role.SPYMASTER })
        assertEquals(1, randomized.players.count { it.team == Team.RED && it.role == Role.OPERATIVE })
        assertEquals(1, randomized.players.count { it.team == Team.BLUE && it.role == Role.SPYMASTER })
        assertEquals(1, randomized.players.count { it.team == Team.BLUE && it.role == Role.OPERATIVE })
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
