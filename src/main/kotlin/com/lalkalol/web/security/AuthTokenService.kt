package com.lalkalol.web.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.lalkalol.config.AppProperties
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class AuthTokenService(
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper,
) {
    private val ttlMs = 7 * 24 * 60 * 60 * 1000L

    data class TokenPayload(
        val playerId: String,
        val roomCode: String,
        val discordUserId: String? = null,
        val exp: Long,
    )

    fun issue(playerId: String, roomCode: String, discordUserId: String? = null): String {
        val payload = TokenPayload(playerId, roomCode, discordUserId, System.currentTimeMillis() + ttlMs)
        val json = objectMapper.writeValueAsString(payload)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray(Charsets.UTF_8))
        val sig = sign(encoded)
        return "$encoded.$sig"
    }

    fun validate(token: String): TokenPayload? {
        val dotIdx = token.lastIndexOf('.')
        if (dotIdx < 0) return null
        val encoded = token.substring(0, dotIdx)
        val sig = token.substring(dotIdx + 1)
        if (sign(encoded) != sig) return null
        val json = runCatching {
            String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        }.getOrNull() ?: return null
        val payload = runCatching {
            objectMapper.readValue(json, TokenPayload::class.java)
        }.getOrNull() ?: return null
        if (System.currentTimeMillis() > payload.exp) return null
        return payload
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appProperties.sessionSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
