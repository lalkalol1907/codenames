package com.lalkalol.config

import com.lalkalol.web.configureApi
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    val indexHtml = loadSpaIndexHtml()

    routing {
        staticResources("/static", "static")
        configureApi()
        spaRoutes(indexHtml)
    }
}
