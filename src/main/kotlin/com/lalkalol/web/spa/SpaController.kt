package com.lalkalol.web.spa

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaController {
    @GetMapping("/", "/rooms/{code}", "/rooms/{code}/game")
    fun spaRoutes(): ResponseEntity<String> = spaResponse()

    fun spaResponse(): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(indexHtml)

    companion object {
        private val indexHtml: String by lazy {
            val resource = ClassPathResource("static/index.html")
            if (resource.exists()) {
                resource.inputStream.bufferedReader().readText()
            } else {
                FALLBACK_SPA_HTML
            }
        }

        private const val FALLBACK_SPA_HTML = """<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><title>Codeword</title></head>
<body><div id="app"></div></body>
</html>"""
    }
}
