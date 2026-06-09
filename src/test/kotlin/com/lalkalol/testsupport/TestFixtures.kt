package com.lalkalol.testsupport

import com.lalkalol.common.model.Language
import com.lalkalol.common.model.Role
import com.lalkalol.common.model.Team
import com.lalkalol.game.service.GameService
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.service.RoomService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class SpringIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var roomService: RoomService

    @Autowired
    lateinit var gameService: GameService
}

data class FourPlayerGame(
    val room: Room,
    val host: Player,
    val redOperative: Player,
    val blueSpymaster: Player,
    val blueOperative: Player,
) {
    val roomCode: String get() = room.code
    val redSpymaster: Player get() = host

    fun spymaster(team: Team): Player = when (team) {
        Team.RED -> host
        Team.BLUE -> blueSpymaster
    }

    fun operative(team: Team): Player = when (team) {
        Team.RED -> redOperative
        Team.BLUE -> blueOperative
    }

    fun startingTeam(): Team = room.requireGame().currentTeam

    fun currentSpymaster(): Player = spymaster(startingTeam())

    fun currentOperative(): Player = operative(startingTeam())

    fun player(id: java.util.UUID): Player =
        room.players.first { it.id == id }
}

fun RoomService.setupFourPlayerGame(language: Language = Language.RU): FourPlayerGame {
    val (room0, host) = createRoom(language, "Host")
    val redOp = joinRoom(room0.code, "RedAgent").second
    val blueSpy = joinRoom(room0.code, "BlueSpy").second
    val blueOp = joinRoom(room0.code, "BlueAgent").second

    setRole(room0.code, host.id, Team.RED, Role.SPYMASTER)
    setRole(room0.code, redOp.id, Team.RED, Role.OPERATIVE)
    setRole(room0.code, blueSpy.id, Team.BLUE, Role.SPYMASTER)
    setRole(room0.code, blueOp.id, Team.BLUE, Role.OPERATIVE)

    val started = startGame(room0.code, host.id)
    requireNotNull(started.game)

    fun find(role: Role, team: Team): Player =
        started.players.first { it.role == role && it.team == team }

    return FourPlayerGame(
        room = started,
        host = find(Role.SPYMASTER, Team.RED),
        redOperative = find(Role.OPERATIVE, Team.RED),
        blueSpymaster = find(Role.SPYMASTER, Team.BLUE),
        blueOperative = find(Role.OPERATIVE, Team.BLUE),
    )
}

fun Room.requireGame() = checkNotNull(game) { "Expected game in room ${code}" }

fun h2TestJdbcUrl(name: String): String =
    "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
