package com.lalkalol.room.web

import com.lalkalol.common.model.Language
import com.lalkalol.common.model.Role
import com.lalkalol.common.model.Team
import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.i18n.PageModel
import com.lalkalol.room.service.RoomException
import com.lalkalol.room.service.RoomService
import com.lalkalol.web.advice.errorResponse
import com.lalkalol.web.advice.exceptionResponse
import com.lalkalol.room.dto.CreateRoomRequest
import com.lalkalol.room.dto.EnumOption
import com.lalkalol.room.dto.JoinRoomInCodeRequest
import com.lalkalol.room.dto.JoinRoomRequest
import com.lalkalol.room.dto.RoomActionResponse
import com.lalkalol.room.dto.RoomBootstrapDto
import com.lalkalol.room.dto.RoomOptionsDto
import com.lalkalol.room.dto.SetRoleRequest
import com.lalkalol.room.dto.ViewBuilder
import com.lalkalol.web.session.PlayerSessionSupport
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/rooms")
class RoomController(
    private val roomService: RoomService,
    private val hub: GameSessionHub,
    private val localeSupport: LocaleSupport,
) {
    @PostMapping
    fun createRoom(
        request: HttpServletRequest,
        httpSession: HttpSession,
        @RequestBody body: CreateRoomRequest,
    ): ResponseEntity<*> {
        val language = Language.fromCode(body.language)
        val hostName = body.name.trim().take(64)
        if (hostName.isEmpty()) {
            return errorResponse(localeSupport, request, "error.enter_name")
        }
        return try {
            val (room, host) = roomService.createRoom(language, hostName)
            PlayerSessionSupport.set(httpSession, host.id.toString(), room.code)
            ResponseEntity.ok(RoomActionResponse(room.code, host.id.toString()))
        } catch (e: RoomException) {
            exceptionResponse(localeSupport, request, e.message)
        }
    }

    @PostMapping("/join")
    fun joinRoomGlobal(
        request: HttpServletRequest,
        httpSession: HttpSession,
        @RequestBody body: JoinRoomRequest,
    ): ResponseEntity<*> {
        val code = body.code.trim().uppercase()
        val name = body.name.trim().take(64)
        if (code.isEmpty() || name.isEmpty()) {
            return errorResponse(localeSupport, request, "error.enter_code_and_name")
        }
        return try {
            val (room, player) = roomService.joinRoom(code, name)
            PlayerSessionSupport.set(httpSession, player.id.toString(), room.code)
            hub.broadcast(room.code)
            ResponseEntity.ok(RoomActionResponse(room.code, player.id.toString()))
        } catch (e: RoomException) {
            exceptionResponse(localeSupport, request, e.message)
        }
    }

    @GetMapping("/{code}")
    fun getRoom(
        request: HttpServletRequest,
        @PathVariable code: String,
    ): ResponseEntity<*> {
        val roomCode = code.uppercase()
        val session = PlayerSessionSupport.resolve(request)
        val room = roomService.getRoom(roomCode)
        if (room == null) {
            return errorResponse(localeSupport, request, "error.room_not_found", HttpStatus.NOT_FOUND)
        }
        if (session == null || !session.roomCode.equals(roomCode, ignoreCase = true)) {
            return ResponseEntity.ok(
                RoomBootstrapDto(
                    code = roomCode,
                    language = room.language.code,
                    needJoin = true,
                ),
            )
        }
        if (room.players.none { it.id == session.playerUuid() }) {
            request.getSession(false)?.let { PlayerSessionSupport.clear(it) }
            return ResponseEntity.ok(
                RoomBootstrapDto(
                    code = roomCode,
                    language = room.language.code,
                    needJoin = true,
                ),
            )
        }
        val view = ViewBuilder.buildRoomView(room, session.playerUuid())
        return ResponseEntity.ok(
            RoomBootstrapDto(
                code = roomCode,
                language = room.language.code,
                needJoin = false,
                view = view,
            ),
        )
    }

    @GetMapping("/{code}/options")
    fun roomOptions(
        request: HttpServletRequest,
        @PathVariable code: String,
    ): RoomOptionsDto {
        val locale = localeSupport.resolve(request)
        return RoomOptionsDto(
            teams = PageModel.teamOptions(locale).map {
                EnumOption(it["value"]!!, it["label"]!!)
            },
            roles = PageModel.roleOptions(locale).map {
                EnumOption(it["value"]!!, it["label"]!!)
            },
        )
    }

    @PostMapping("/{code}/join")
    fun joinRoom(
        request: HttpServletRequest,
        httpSession: HttpSession,
        @PathVariable code: String,
        @RequestBody body: JoinRoomInCodeRequest,
    ): ResponseEntity<*> {
        val roomCode = code.uppercase()
        val name = body.name.trim().take(64)
        if (name.isEmpty()) {
            return errorResponse(localeSupport, request, "error.enter_name")
        }
        return try {
            val (room, player) = roomService.joinRoom(roomCode, name)
            PlayerSessionSupport.set(httpSession, player.id.toString(), room.code)
            hub.broadcast(room.code)
            ResponseEntity.ok(RoomActionResponse(room.code, player.id.toString()))
        } catch (e: RoomException) {
            exceptionResponse(localeSupport, request, e.message)
        }
    }

    @PostMapping("/{code}/role")
    fun setRole(
        request: HttpServletRequest,
        @PathVariable code: String,
        @RequestBody body: SetRoleRequest,
    ): ResponseEntity<*> {
        val roomCode = code.uppercase()
        val session = PlayerSessionSupport.resolve(request)
        if (session == null) {
            return errorResponse(localeSupport, request, "error.player_not_in_room", HttpStatus.UNAUTHORIZED)
        }
        val team = parseTeam(body.team)
        val role = parseRole(body.role)
        if (role == null || (role != Role.SPECTATOR && team == null)) {
            return errorResponse(localeSupport, request, "error.invalid_role")
        }
        return try {
            roomService.setRole(roomCode, session.playerUuid(), team, role)
            hub.broadcast(roomCode)
            ResponseEntity.noContent().build<Void>()
        } catch (e: RoomException) {
            exceptionResponse(localeSupport, request, e.message)
        }
    }

    @PostMapping("/{code}/randomize")
    fun randomizeTeams(
        request: HttpServletRequest,
        @PathVariable code: String,
    ): ResponseEntity<*> {
        val roomCode = code.uppercase()
        val session = PlayerSessionSupport.resolve(request)
        if (session == null) {
            return errorResponse(localeSupport, request, "error.player_not_in_room", HttpStatus.UNAUTHORIZED)
        }
        return try {
            roomService.randomizeTeams(roomCode, session.playerUuid())
            hub.broadcast(roomCode)
            ResponseEntity.noContent().build<Void>()
        } catch (e: RoomException) {
            exceptionResponse(localeSupport, request, e.message)
        }
    }

    @PostMapping("/{code}/start")
    fun startGame(
        request: HttpServletRequest,
        @PathVariable code: String,
    ): ResponseEntity<*> {
        val roomCode = code.uppercase()
        val session = PlayerSessionSupport.resolve(request)
        if (session == null) {
            return errorResponse(localeSupport, request, "error.player_not_in_room", HttpStatus.UNAUTHORIZED)
        }
        return try {
            roomService.startGame(roomCode, session.playerUuid())
            hub.broadcast(roomCode)
            ResponseEntity.noContent().build<Void>()
        } catch (e: RoomException) {
            exceptionResponse(localeSupport, request, e.message)
        }
    }

    private fun parseTeam(value: String?): Team? =
        value?.let { runCatching { Team.valueOf(it) }.getOrNull() }

    private fun parseRole(value: String?): Role? =
        value?.let { runCatching { Role.valueOf(it) }.getOrNull() }
}
