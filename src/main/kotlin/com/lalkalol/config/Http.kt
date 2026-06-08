package com.lalkalol.config

import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun Application.configureHttp() {
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            },
        )
    }

    if (!appSettings().exposeApiDocs) {
        return
    }

    install(AsyncApiPlugin) {
        extension = AsyncApiExtension.builder {
            info {
                title("Codenames")
                version("1.0.0")
            }
        }
    }
    routing {
        openAPI(path = "openapi") {
            // Served from src/main/resources/documentation.yaml
        }
    }
}
