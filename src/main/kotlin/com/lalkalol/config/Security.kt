package com.lalkalol.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.matchContentType
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.minutes

fun Application.configureSecurity() {
    val settings = appSettings()

    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        if (settings.isProduction) {
            header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }
    }

    install(Compression) {
        gzip {
            matchContentType(
                io.ktor.http.ContentType.Text.Any,
                io.ktor.http.ContentType.Application.Json,
                io.ktor.http.ContentType.Text.CSS,
                io.ktor.http.ContentType.Application.JavaScript,
            )
        }
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(RateLimit) {
        register(RateLimitName("room-actions")) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
        }
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}
