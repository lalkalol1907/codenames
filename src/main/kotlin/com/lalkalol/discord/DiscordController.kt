package com.lalkalol.discord

import com.lalkalol.common.model.Language
import com.lalkalol.room.dto.RoomViewDto
import com.lalkalol.room.dto.ViewBuilder
import com.lalkalol.room.service.RoomException
import com.lalkalol.room.service.RoomService
import com.lalkalol.room.web.GameSessionHub
import com.lalkalol.web.security.AuthTokenService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class DiscordBootstrapRequest(
    val code: String,
    val instanceId: String,
    val channelId: String,
    val language: String = "en",
)

data class DiscordBootstrapResponse(
    val appToken: String,
    val discordAccessToken: String,
    val roomCode: String,
    val view: RoomViewDto,
)

@RestController
@RequestMapping("/api/discord")
class DiscordController(
    private val roomService: RoomService,
    private val hub: GameSessionHub,
    private val authTokenService: AuthTokenService,
    private val discordApiClient: DiscordApiClient,
) {
    @PostMapping("/bootstrap")
    fun bootstrap(
        request: HttpServletRequest,
        @RequestBody body: DiscordBootstrapRequest,
    ): ResponseEntity<*> {
        if (body.code.isBlank() || body.instanceId.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Missing required fields"))
        }

        val tokenResponse = discordApiClient.exchangeCode(body.code)
            ?: return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(mapOf("error" to "Failed to exchange Discord code"))

        val discordUser = discordApiClient.getUser(tokenResponse.accessToken)
            ?: return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(mapOf("error" to "Failed to fetch Discord user"))

        val language = Language.fromCode(body.language)
        return try {
            val (room, player) = roomService.bootstrapDiscordRoom(
                instanceId = body.instanceId,
                channelId = body.channelId,
                discordUser = discordUser,
                language = language,
            )
            hub.broadcast(room.code)
            val appToken = authTokenService.issue(player.id.toString(), room.code, discordUser.id)
            val view = ViewBuilder.buildRoomView(room, player.id)
            ResponseEntity.ok(
                DiscordBootstrapResponse(
                    appToken = appToken,
                    discordAccessToken = tokenResponse.accessToken,
                    roomCode = room.code,
                    view = view,
                ),
            )
        } catch (e: RoomException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Room error")))
        }
    }
}
