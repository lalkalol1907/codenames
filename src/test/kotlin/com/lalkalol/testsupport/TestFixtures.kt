package com.lalkalol.testsupport

import com.lalkalol.config.configureDatabase
import com.lalkalol.config.configureDependencyInjection
import com.lalkalol.config.configureRouting
import com.lalkalol.config.configureSessions
import com.lalkalol.config.configureTemplates
import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.Team
import com.lalkalol.game.service.GameService
import com.lalkalol.room.model.Player
import com.lalkalol.room.model.Room
import com.lalkalol.room.service.RoomService
import io.ktor.server.application.Application
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

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

fun ApplicationTestBuilder.configureTestEnvironment() {
    environment {
        config = MapApplicationConfig(
            "database.driver" to "org.h2.Driver",
            "database.url" to "jdbc:h2:mem:codenames-${System.nanoTime()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            "database.user" to "sa",
            "database.password" to "",
        )
    }
}

fun withTestApp(block: suspend Application.() -> Unit) {
    testApplication {
        configureTestEnvironment()
        application {
            configureDependencyInjection()
            configureDatabase()
        }
        startApplication()
        application.block()
    }
}

fun withTestServer(block: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication {
        configureTestEnvironment()
        application {
            configureDependencyInjection()
            configureDatabase()
            configureTemplates()
            configureSessions()
            configureRouting()
        }
        startApplication()
        block()
    }
}

suspend fun Application.roomService(): RoomService = dependencies.resolve()

suspend fun Application.gameService(): GameService = dependencies.resolve()

suspend fun RoomService.setupFourPlayerGame(language: Language = Language.RU): FourPlayerGame {
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
