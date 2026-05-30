package com.lalkalol.web

import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.RoomStatus
import com.lalkalol.game.model.Team
import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.i18n.Messages
import com.lalkalol.i18n.PageModel
import com.lalkalol.i18n.PageModel.redirectWithError
import com.lalkalol.i18n.PageModel.redirectWithException
import com.lalkalol.i18n.UiLocale
import com.lalkalol.room.service.RoomService
import com.lalkalol.room.service.RoomException
import com.lalkalol.web.Csrf
import com.lalkalol.web.GameSessionHub
import com.lalkalol.web.dto.ViewBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.configurePages() {
    val json = Json { encodeDefaults = true }

    routing {
        post("/locale") {
            val params = call.receiveParameters()
            if (!Csrf.validate(call, params)) {
                call.respondRedirect("/")
                return@post
            }
            val locale = UiLocale.fromCode(params["locale"] ?: "") ?: UiLocale.default()
            LocaleSupport.setCookie(call, locale)
            val redirect = params["redirect"]?.takeIf { it.startsWith("/") } ?: "/"
            call.respondRedirect(redirect)
        }

        get("/") {
            val locale = LocaleSupport.resolve(call)
            call.respond(
                FreeMarkerContent(
                    "index.ftl",
                    PageModel.base(
                        call,
                        mapOf(
                            "error" to call.request.queryParameters["error"],
                            "gameLanguages" to Language.entries.map { lang ->
                                mapOf(
                                    "value" to lang.code,
                                    "label" to Messages.t(locale, "lang.${lang.code}"),
                                )
                            },
                        ),
                    ),
                ),
            )
        }

        rateLimit(RateLimitName("room-actions")) {
            post("/rooms") {
                val params = call.receiveParameters()
                if (!Csrf.validate(call, params)) {
                    call.redirectWithError("/", "error.csrf")
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val language = Language.fromCode(params["language"] ?: "en")
                val hostName = params["name"]?.trim().orEmpty().take(64)
                if (hostName.isEmpty()) {
                    call.redirectWithError("/", "error.enter_name")
                    return@post
                }
                try {
                    val (room, host) = roomService.createRoom(language, hostName)
                    call.sessions.set(PlayerSession(host.id.toString(), room.code))
                    call.respondRedirect("/rooms/${room.code}")
                } catch (e: RoomException) {
                    call.redirectWithException("/", e.message)
                }
            }

            post("/rooms/join") {
                val params = call.receiveParameters()
                if (!Csrf.validate(call, params)) {
                    call.redirectWithError("/", "error.csrf")
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val code = params["code"]?.trim().orEmpty().uppercase()
                val name = params["name"]?.trim().orEmpty().take(64)
                if (code.isEmpty() || name.isEmpty()) {
                    call.redirectWithError("/", "error.enter_code_and_name")
                    return@post
                }
                try {
                    val (room, player) = roomService.joinRoom(code, name)
                    call.sessions.set(PlayerSession(player.id.toString(), room.code))
                    application.dependencies.resolve<GameSessionHub>().broadcast(room.code)
                    call.respondRedirect("/rooms/${room.code}")
                } catch (e: RoomException) {
                    call.redirectWithException("/", e.message)
                }
            }
        }

        route("/rooms/{code}") {
            get {
                val roomService = application.dependencies.resolve<RoomService>()
                val code = call.parameters["code"]!!.uppercase()
                val session = call.sessions.get<PlayerSession>()
                val locale = LocaleSupport.resolve(call)
                val room = roomService.getRoom(code)
                if (room == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        FreeMarkerContent(
                            "index.ftl",
                            PageModel.base(
                                call,
                                mapOf(
                                    "error" to Messages.t(locale, "error.room_not_found"),
                                    "gameLanguages" to Language.entries.map { lang ->
                                        mapOf(
                                            "value" to lang.code,
                                            "label" to Messages.t(locale, "lang.${lang.code}"),
                                        )
                                    },
                                ),
                            ),
                        ),
                    )
                    return@get
                }
                if (session == null || !session.roomCode.equals(code, ignoreCase = true)) {
                    call.respond(
                        FreeMarkerContent(
                            "join.ftl",
                            PageModel.base(call, mapOf("code" to code, "error" to null)),
                        ),
                    )
                    return@get
                }
                if (room.players.none { it.id == session.playerUuid() }) {
                    call.sessions.clear<PlayerSession>()
                    call.respond(
                        FreeMarkerContent(
                            "join.ftl",
                            PageModel.base(call, mapOf("code" to code, "error" to null)),
                        ),
                    )
                    return@get
                }
                if (room.status == RoomStatus.PLAYING || room.status == RoomStatus.FINISHED) {
                    call.respondRedirect("/rooms/$code/game")
                    return@get
                }
                val viewerId = session.playerUuid()
                val view = ViewBuilder.buildRoomView(room, viewerId)
                call.respond(
                    FreeMarkerContent(
                        "lobby.ftl",
                        PageModel.base(
                            call,
                            mapOf(
                                "room" to room,
                                "viewJson" to json.encodeToString(view),
                                "teams" to PageModel.teamOptions(locale),
                                "roles" to PageModel.roleOptions(locale),
                                "error" to call.request.queryParameters["error"],
                            ),
                        ),
                    ),
                )
            }

            post("/join") {
                val params = call.receiveParameters()
                if (!Csrf.validate(call, params)) {
                    call.redirectWithError("/rooms/${call.parameters["code"]}", "error.csrf")
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val hub = application.dependencies.resolve<GameSessionHub>()
                val code = call.parameters["code"]!!.uppercase()
                val locale = LocaleSupport.resolve(call)
                val name = params["name"]?.trim().orEmpty().take(64)
                if (name.isEmpty()) {
                    call.redirectWithError("/rooms/$code", "error.enter_name")
                    return@post
                }
                try {
                    val (room, player) = roomService.joinRoom(code, name)
                    call.sessions.set(PlayerSession(player.id.toString(), room.code))
                    hub.broadcast(room.code)
                    call.respondRedirect("/rooms/${room.code}")
                } catch (e: RoomException) {
                    call.respond(
                        FreeMarkerContent(
                            "join.ftl",
                            PageModel.base(
                                call,
                                mapOf(
                                    "code" to code,
                                    "error" to Messages.translateException(locale, e.message),
                                ),
                            ),
                        ),
                    )
                }
            }

            post("/role") {
                val params = call.receiveParameters()
                if (!Csrf.validate(call, params)) {
                    call.redirectWithError("/rooms/${call.parameters["code"]}", "error.csrf")
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val hub = application.dependencies.resolve<GameSessionHub>()
                val code = call.parameters["code"]!!.uppercase()
                val session = call.sessions.get<PlayerSession>()
                    ?: return@post call.respondRedirect("/rooms/$code")
                val team = parseTeam(params["team"])
                val role = parseRole(params["role"])
                if (team == null || role == null) {
                    call.redirectWithError("/rooms/$code", "error.invalid_role")
                    return@post
                }
                try {
                    roomService.setRole(code, session.playerUuid(), team, role)
                    hub.broadcast(code)
                    call.respondRedirect("/rooms/$code")
                } catch (e: RoomException) {
                    call.redirectWithException("/rooms/$code", e.message)
                }
            }

            post("/randomize") {
                val params = call.receiveParameters()
                if (!Csrf.validate(call, params)) {
                    call.redirectWithError("/rooms/${call.parameters["code"]}", "error.csrf")
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val hub = application.dependencies.resolve<GameSessionHub>()
                val code = call.parameters["code"]!!.uppercase()
                val session = call.sessions.get<PlayerSession>()
                    ?: return@post call.respondRedirect("/rooms/$code")
                try {
                    roomService.randomizeTeams(code, session.playerUuid())
                    hub.broadcast(code)
                    call.respondRedirect("/rooms/$code")
                } catch (e: RoomException) {
                    call.redirectWithException("/rooms/$code", e.message)
                }
            }

            post("/start") {
                val params = call.receiveParameters()
                if (!Csrf.validate(call, params)) {
                    call.redirectWithError("/rooms/${call.parameters["code"]}", "error.csrf")
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val hub = application.dependencies.resolve<GameSessionHub>()
                val code = call.parameters["code"]!!.uppercase()
                val session = call.sessions.get<PlayerSession>()
                    ?: return@post call.respondRedirect("/rooms/$code")
                try {
                    roomService.startGame(code, session.playerUuid())
                    hub.broadcast(code)
                    call.respondRedirect("/rooms/$code/game")
                } catch (e: RoomException) {
                    call.redirectWithException("/rooms/$code", e.message)
                }
            }

            get("/game") {
                val roomService = application.dependencies.resolve<RoomService>()
                val code = call.parameters["code"]!!.uppercase()
                val session = call.sessions.get<PlayerSession>()
                    ?: return@get call.respondRedirect("/rooms/$code")
                val room = roomService.getRoom(code)
                if (room == null) {
                    call.respondRedirect("/")
                    return@get
                }
                if (room.status == RoomStatus.LOBBY) {
                    call.respondRedirect("/rooms/$code")
                    return@get
                }
                val view = ViewBuilder.buildRoomView(room, session.playerUuid())
                call.respond(
                    FreeMarkerContent(
                        "game.ftl",
                        PageModel.base(
                            call,
                            mapOf(
                                "viewJson" to json.encodeToString(view),
                                "roomCode" to code,
                            ),
                        ),
                    ),
                )
            }
        }
    }
}

private fun parseTeam(value: String?): Team? =
    value?.let { runCatching { Team.valueOf(it) }.getOrNull() }

private fun parseRole(value: String?): Role? =
    value?.let { runCatching { Role.valueOf(it) }.getOrNull() }
