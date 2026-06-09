package com.lalkalol.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val environment: String = "dev",
    val publicUrl: String = "http://localhost:8080",
    val secureCookies: Boolean = false,
    val sessionSecret: String = "dev-secret-change-me-in-production",
    val exposeApiDocs: Boolean = true,
) {
    val isProduction: Boolean get() = environment.equals("prod", ignoreCase = true)
}
