package com.lalkalol.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val environment: String = "dev",
    val publicUrl: String = "http://localhost:8080",
    val secureCookies: Boolean = false,
    val sessionSecret: String = "dev-secret-change-me-in-production",
    val exposeApiDocs: Boolean = true,
    val discord: DiscordProperties = DiscordProperties(),
    val embed: EmbedProperties = EmbedProperties(),
    val cleanup: CleanupProperties = CleanupProperties(),
) {
    val isProduction: Boolean get() = environment.equals("prod", ignoreCase = true)
}

data class DiscordProperties(
    val clientId: String = "",
    val clientSecret: String = "",
)

data class EmbedProperties(
    val frameAncestors: String = "",
)

data class CleanupProperties(
    val enabled: Boolean = true,
    val roomRetentionDays: Long = 30,
    val cron: String = "0 0 3 * * *",
)
