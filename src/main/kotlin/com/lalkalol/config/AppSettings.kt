package com.lalkalol.config

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.util.AttributeKey

data class AppSettings(
    val environment: String,
    val publicUrl: String,
    val secureCookies: Boolean,
    val sessionSecret: String,
    val exposeApiDocs: Boolean,
) {
    val isProduction: Boolean get() = environment.equals("prod", ignoreCase = true)
}

private val AppSettingsKey = AttributeKey<AppSettings>("AppSettings")

fun Application.appSettings(): AppSettings =
    attributes[AppSettingsKey]

fun Application.configureAppSettings() {
    val config = environment.config.config("app")
    attributes.put(AppSettingsKey, appSettingsFrom(config))
}

private fun ApplicationConfig.propertyOr(key: String, default: String): String =
    propertyOrNull(key)?.getString() ?: default

private fun ApplicationConfig.propertyOr(key: String, default: Boolean): Boolean =
    propertyOrNull(key)?.getString()?.toBooleanStrictOrNull() ?: default

private fun appSettingsFrom(config: ApplicationConfig): AppSettings =
    AppSettings(
        environment = envOrConfig("APP_ENV", config, "environment", "dev"),
        publicUrl = envOrConfig("APP_PUBLIC_URL", config, "publicUrl", "http://localhost:8080"),
        secureCookies = envOrConfig("APP_SECURE_COOKIES", config, "secureCookies", "false").toBooleanStrictOrNull()
            ?: config.propertyOr("secureCookies", false),
        sessionSecret = envOrConfig("SESSION_SECRET", config, "sessionSecret", "dev-secret-change-me-in-production"),
        exposeApiDocs = envOrConfig("APP_EXPOSE_API_DOCS", config, "exposeApiDocs", "true").toBooleanStrictOrNull()
            ?: config.propertyOr("exposeApiDocs", true),
    )

private fun envOrConfig(envKey: String, config: ApplicationConfig, configKey: String, default: String): String =
    System.getenv(envKey)?.takeIf { it.isNotBlank() }
        ?: config.propertyOr(configKey, default)
