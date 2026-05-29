package com.lalkalol.room.service

import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import com.lalkalol.testsupport.roomService
import com.lalkalol.testsupport.setupFourPlayerGame
import com.lalkalol.testsupport.withTestApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomServiceTest {

    @Test
    fun `createRoom assigns host`() = withTestApp {
        val roomService = roomService()
        val (room, host) = roomService.createRoom(Language.RU, "Alice")

        assertEquals(RoomStatus.LOBBY, room.status)
        assertEquals(1, room.players.size)
        assertTrue(host.isHost)
        assertEquals(4, room.code.length)
    }

    @Test
    fun `joinRoom adds player to lobby`() = withTestApp {
        val roomService = roomService()
        val (room, _) = roomService.createRoom(Language.EN, "Host")
        val (updated, guest) = roomService.joinRoom(room.code, "Guest")

        assertEquals(2, updated.players.size)
        assertEquals("Guest", guest.name)
        assertEquals(RoomStatus.LOBBY, updated.status)
    }

    @Test
    fun `duplicate role assignment is rejected`() = withTestApp {
        val roomService = roomService()
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val (_, guest) = roomService.joinRoom(room.code, "Guest")

        roomService.setRole(room.code, host.id, Team.RED, Role.SPYMASTER)
        assertFailsWith<RoomException> {
            roomService.setRole(room.code, guest.id, Team.RED, Role.SPYMASTER)
        }
    }

    @Test
    fun `startGame requires four players with valid roles`() = withTestApp {
        val roomService = roomService()
        val (room, host) = roomService.createRoom(Language.RU, "Host")

        assertFailsWith<RoomException> {
            roomService.startGame(room.code, host.id)
        }

        val setup = roomService.setupFourPlayerGame()
        assertEquals(RoomStatus.PLAYING, setup.room.status)
        assertNotNull(setup.room.game)
        assertEquals(25, setup.room.game!!.cards.size)
    }

    @Test
    fun `randomizeTeams assigns all roles for four players`() = withTestApp {
        val roomService = roomService()
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
    fun `leaveRoom deletes empty room`() = withTestApp {
        val roomService = roomService()
        val (room, host) = roomService.createRoom(Language.RU, "Solo")

        val afterLeave = roomService.leaveRoom(room.code, host.id)

        assertNull(afterLeave)
        assertNull(roomService.getRoom(room.code))
    }

    @Test
    fun `leaveRoom transfers host`() = withTestApp {
        val roomService = roomService()
        val (room, host) = roomService.createRoom(Language.RU, "Host")
        val (_, guest) = roomService.joinRoom(room.code, "Guest")

        roomService.leaveRoom(room.code, host.id)
        val updated = roomService.getRoom(room.code)

        assertNotNull(updated)
        assertEquals(guest.id, updated.hostPlayerId)
        assertTrue(updated.players.single().isHost)
    }
}
