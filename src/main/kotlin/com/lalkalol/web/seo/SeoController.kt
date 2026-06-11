package com.lalkalol.web.seo

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SeoController {
    @GetMapping("/robots.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun robotsTxt(): ResponseEntity<String> = serveText("static/robots.txt", FALLBACK_ROBOTS)

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun sitemapXml(): ResponseEntity<String> = serveText("static/sitemap.xml", FALLBACK_SITEMAP)

    private fun serveText(classpath: String, fallback: String): ResponseEntity<String> {
        val resource = ClassPathResource(classpath)
        val body =
            if (resource.exists()) {
                resource.inputStream.bufferedReader().readText()
            } else {
                fallback
            }
        return ResponseEntity.ok().body(body)
    }

    companion object {
        private const val FALLBACK_ROBOTS = """
User-agent: *
Allow: /
Disallow: /rooms/
Disallow: /openapi
Disallow: /ws/

Sitemap: https://codewords.ru/sitemap.xml
"""

        private const val FALLBACK_SITEMAP = """<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://codewords.ru/</loc>
  </url>
</urlset>
"""
    }
}
