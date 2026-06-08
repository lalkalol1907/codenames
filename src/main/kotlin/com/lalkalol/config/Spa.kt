package com.lalkalol.config

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private const val FALLBACK_SPA_HTML = """<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><title>Codeword</title></head>
<body><div id="app"></div></body>
</html>"""

fun Application.loadSpaIndexHtml(): String =
    javaClass.classLoader.getResourceAsStream("static/index.html")?.bufferedReader()?.readText()
        ?: FALLBACK_SPA_HTML

fun Route.spaRoutes(indexHtml: String) {
    get("/") {
        call.respondText(indexHtml, ContentType.Text.Html)
    }

    get("/rooms/{code}") {
        call.respondText(indexHtml, ContentType.Text.Html)
    }

    get("/rooms/{code}/game") {
        call.respondText(indexHtml, ContentType.Text.Html)
    }

    get("/{...}") {
        call.respondText(indexHtml, ContentType.Text.Html)
    }
}
