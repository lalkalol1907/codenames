package com.lalkalol.web

import com.lalkalol.game.model.Language
import com.lalkalol.game.model.Role
import com.lalkalol.game.model.Team
import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.i18n.Messages
import com.lalkalol.i18n.PageModel
import com.lalkalol.i18n.UiLocale
import com.lalkalol.room.service.RoomException
import com.lalkalol.room.service.RoomService
import com.lalkalol.web.dto.CreateRoomRequest
import com.lalkalol.web.dto.CsrfResponse
import com.lalkalol.web.dto.ErrorResponse
import com.lalkalol.web.dto.JoinRoomInCodeRequest
import com.lalkalol.web.dto.JoinRoomRequest
import com.lalkalol.web.dto.RoomActionResponse
import com.lalkalol.web.dto.RoomBootstrapDto
import com.lalkalol.web.dto.RoomOptionsDto
import com.lalkalol.web.dto.SessionResponse
import com.lalkalol.web.dto.SetLocaleRequest
import com.lalkalol.web.dto.SetRoleRequest
import com.lalkalol.web.dto.ViewBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.routing.application
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.set
import io.ktor.server.sessions.sessions

fun Application.configureApi() {
    routing {
        get("/api/csrf") {
            val token = Csrf.token(call)
            call.respond(CsrfResponse(token))
        }

        get("/api/session") {
            val session = call.sessions.get<PlayerSession>()
            if (session == null) {
                call.respond(SessionResponse())
            } else {
                call.respond(SessionResponse(session.playerId, session.roomCode))
            }
        }

        get("/api/i18n") {
            val localeParam = call.request.queryParameters["locale"]
            val locale = UiLocale.fromCode(localeParam ?: "") ?: LocaleSupport.resolve(call)
            call.respond(Messages.allMessages(locale))
        }

        post("/api/locale") {
            if (!Csrf.validateApi(call)) {
                respondError(call, "error.csrf", HttpStatusCode.Forbidden)
                return@post
            }
            val body = call.receive<SetLocaleRequest>()
            val locale = UiLocale.fromCode(body.locale) ?: UiLocale.default()
            LocaleSupport.setCookie(call, locale)
            call.respond(HttpStatusCode.NoContent)
        }

        rateLimit(RateLimitName("room-actions")) {
            post("/api/rooms") {
                if (!Csrf.validateApi(call)) {
                    respondError(call, "error.csrf", HttpStatusCode.Forbidden)
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val body = call.receive<CreateRoomRequest>()
                val language = Language.fromCode(body.language)
                val hostName = body.name.trim().take(64)
                if (hostName.isEmpty()) {
                    respondError(call, "error.enter_name")
                    return@post
                }
                try {
                    val (room, host) = roomService.createRoom(language, hostName)
                    call.sessions.set(PlayerSession(host.id.toString(), room.code))
                    call.respond(RoomActionResponse(room.code, host.id.toString()))
                } catch (e: RoomException) {
                    respondException(call, e.message)
                }
            }

            post("/api/rooms/join") {
                if (!Csrf.validateApi(call)) {
                    respondError(call, "error.csrf", HttpStatusCode.Forbidden)
                    return@post
                }
                val roomService = application.dependencies.resolve<RoomService>()
                val hub = application.dependencies.resolve<GameSessionHub>()
                val body = call.receive<JoinRoomRequest>()
                val code = body.code.trim().uppercase()
                val name = body.name.trim().take(64)
                if (code.isEmpty() || name.isEmpty()) {
                    respondError(call, "error.enter_code_and_name")
                    return@post
                }
                try {
                    val (room, player) = roomService.joinRoom(code, name)
                    call.sessions.set(PlayerSession(player.id.toString(), room.code))
                    hub.broadcast(room.code)
                    call.respond(RoomActionResponse(room.code, player.id.toString()))
                } catch (e: RoomException) {
                    respondException(call, e.message)
                }
            }
        }

        route("/api/rooms/{code}") {
            get {
                val roomService = application.dependencies.resolve<RoomService>()
                val code = call.parameters["code"]!!.uppercase()
                val session = call.sessions.get<PlayerSession>()
                val room = roomService.getRoom(code)
                if (room == null) {
                    respondError(call, "error.room_not_found", HttpStatusCode.NotFound)
                    return@get
                }
                if (session == null || !session.roomCode.equals(code, ignoreCase = true)) {
                    call.respond(
                        RoomBootstrapDto(
                            code = code,
                            language = room.language.code,
                            needJoin = true,
                        ),
                    )
                    return@get
                }
                if (room.players.none { it.id == session.playerUuid() }) {
                    call.sessions.clear<PlayerSession>()
                    call.respond(
                        RoomBootstrapDto(
                            code = code,
                            language = room.language.code,
                            needJoin = true,
                        ),
                    )
                    return@get
                }
                val view = ViewBuilder.buildRoomView(room, session.playerUuid())
                call.respond(
                    RoomBootstrapDto(
                        code = code,
                        language = room.language.code,
                        needJoin = false,
                        view = view,
                    ),
                )
            }

            get("/options") {
                val locale = LocaleSupport.resolve(call)
                call.respond(
                    RoomOptionsDto(
                        teams = PageModel.teamOptions(locale).map {
                            com.lalkalol.web.dto.EnumOption(it["value"]!!, it["label"]!!)
                        },
                        roles = PageModel.roleOptions(locale).map {
                            com.lalkalol.web.dto.EnumOption(it["value"]!!, it["label"]!!)
                        },
                    ),
                )
            }

            rateLimit(RateLimitName("room-actions")) {
                post("/join") {
                    if (!Csrf.validateApi(call)) {
                        respondError(call, "error.csrf", HttpStatusCode.Forbidden)
                        return@post
                    }
                    val roomService = application.dependencies.resolve<RoomService>()
                    val hub = application.dependencies.resolve<GameSessionHub>()
                    val code = call.parameters["code"]!!.uppercase()
                    val body = call.receive<JoinRoomInCodeRequest>()
                    val name = body.name.trim().take(64)
                    if (name.isEmpty()) {
                        respondError(call, "error.enter_name")
                        return@post
                    }
                    try {
                        val (room, player) = roomService.joinRoom(code, name)
                        call.sessions.set(PlayerSession(player.id.toString(), room.code))
                        hub.broadcast(room.code)
                        call.respond(RoomActionResponse(room.code, player.id.toString()))
                    } catch (e: RoomException) {
                        respondException(call, e.message)
                    }
                }

                post("/role") {
                    if (!Csrf.validateApi(call)) {
                        respondError(call, "error.csrf", HttpStatusCode.Forbidden)
                        return@post
                    }
                    val roomService = application.dependencies.resolve<RoomService>()
                    val hub = application.dependencies.resolve<GameSessionHub>()
                    val code = call.parameters["code"]!!.uppercase()
                    val session = call.sessions.get<PlayerSession>()
                    if (session == null) {
                        respondError(call, "error.player_not_in_room", HttpStatusCode.Unauthorized)
                        return@post
                    }
                    val body = call.receive<SetRoleRequest>()
                    val team = parseTeam(body.team)
                    val role = parseRole(body.role)
                    if (team == null || role == null) {
                        respondError(call, "error.invalid_role")
                        return@post
                    }
                    try {
                        roomService.setRole(code, session.playerUuid(), team, role)
                        hub.broadcast(code)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: RoomException) {
                        respondException(call, e.message)
                    }
                }

                post("/randomize") {
                    if (!Csrf.validateApi(call)) {
                        respondError(call, "error.csrf", HttpStatusCode.Forbidden)
                        return@post
                    }
                    val roomService = application.dependencies.resolve<RoomService>()
                    val hub = application.dependencies.resolve<GameSessionHub>()
                    val code = call.parameters["code"]!!.uppercase()
                    val session = call.sessions.get<PlayerSession>()
                    if (session == null) {
                        respondError(call, "error.player_not_in_room", HttpStatusCode.Unauthorized)
                        return@post
                    }
                    try {
                        roomService.randomizeTeams(code, session.playerUuid())
                        hub.broadcast(code)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: RoomException) {
                        respondException(call, e.message)
                    }
                }

                post("/start") {
                    if (!Csrf.validateApi(call)) {
                        respondError(call, "error.csrf", HttpStatusCode.Forbidden)
                        return@post
                    }
                    val roomService = application.dependencies.resolve<RoomService>()
                    val hub = application.dependencies.resolve<GameSessionHub>()
                    val code = call.parameters["code"]!!.uppercase()
                    val session = call.sessions.get<PlayerSession>()
                    if (session == null) {
                        respondError(call, "error.player_not_in_room", HttpStatusCode.Unauthorized)
                        return@post
                    }
                    try {
                        roomService.startGame(code, session.playerUuid())
                        hub.broadcast(code)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: RoomException) {
                        respondException(call, e.message)
                    }
                }
            }
        }
    }
}

private suspend fun respondError(
    call: io.ktor.server.application.ApplicationCall,
    errorKey: String,
    status: HttpStatusCode = HttpStatusCode.BadRequest,
) {
    val locale = LocaleSupport.resolve(call)
    call.respond(status, ErrorResponse(Messages.t(locale, errorKey)))
}

private suspend fun respondException(
    call: io.ktor.server.application.ApplicationCall,
    message: String?,
    status: HttpStatusCode = HttpStatusCode.BadRequest,
) {
    val locale = LocaleSupport.resolve(call)
    call.respond(status, ErrorResponse(Messages.translateException(locale, message)))
}

private fun parseTeam(value: String?): Team? =
    value?.let { runCatching { Team.valueOf(it) }.getOrNull() }

private fun parseRole(value: String?): Role? =
    value?.let { runCatching { Role.valueOf(it) }.getOrNull() }
