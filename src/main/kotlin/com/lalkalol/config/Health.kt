package com.lalkalol.config

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking

fun Application.configureHealth() {
    routing {
        get("/health") {
            call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
        }
        get("/health/ready") {
            val dbOk = runBlocking { checkDatabase() }
            if (!dbOk) {
                call.respondText(
                    """{"status":"degraded","database":"down"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.ServiceUnavailable,
                )
                return@get
            }
            call.respondText("""{"status":"ok","database":"up"}""", ContentType.Application.Json)
        }
    }
}
