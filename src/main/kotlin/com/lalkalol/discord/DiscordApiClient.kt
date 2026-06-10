package com.lalkalol.discord

import com.fasterxml.jackson.annotation.JsonProperty
import com.lalkalol.config.AppProperties
import com.lalkalol.room.service.RoomService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

data class DiscordTokenResponse(
    val accessToken: String,
)

private data class DiscordOAuthTokenDto(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
)

private data class DiscordUserDto(
    val id: String,
    val username: String,
    val avatar: String?,
)

@Component
class DiscordApiClient(
    private val appProperties: AppProperties,
) {
    private val log = LoggerFactory.getLogger(DiscordApiClient::class.java)
    private val restClient = RestClient.create()

    fun exchangeCode(code: String): DiscordTokenResponse? {
        if (appProperties.discord.clientId.isBlank() || appProperties.discord.clientSecret.isBlank()) {
            log.warn("Discord client ID/secret not configured")
            return null
        }
        return runCatching {
            val form = LinkedMultiValueMap<String, String>().apply {
                add("client_id", appProperties.discord.clientId)
                add("client_secret", appProperties.discord.clientSecret)
                add("grant_type", "authorization_code")
                add("code", code)
            }
            val dto = restClient.post()
                .uri("https://discord.com/api/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(form)
                .retrieve()
                .body(DiscordOAuthTokenDto::class.java)!!
            DiscordTokenResponse(accessToken = dto.accessToken)
        }.getOrElse {
            log.error("Discord token exchange failed", it)
            null
        }
    }

    fun getUser(accessToken: String): RoomService.DiscordUser? {
        return runCatching {
            val dto = restClient.get()
                .uri("https://discord.com/api/users/@me")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(DiscordUserDto::class.java)!!
            val avatarUrl = if (dto.avatar != null) {
                "https://cdn.discordapp.com/avatars/${dto.id}/${dto.avatar}.webp?size=64"
            } else {
                val index = dto.id.toLongOrNull()?.and(0x7FFFFFFF)?.rem(5) ?: 0
                "https://cdn.discordapp.com/embed/avatars/$index.png"
            }
            RoomService.DiscordUser(
                id = dto.id,
                username = dto.username,
                avatarUrl = avatarUrl,
            )
        }.getOrElse {
            log.error("Discord user fetch failed", it)
            null
        }
    }
}
