package com.lalkalol.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource

@RestController
class HealthController(
    private val dataSource: DataSource,
) {
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("status" to "ok"))

    @GetMapping("/health/ready")
    fun ready(): ResponseEntity<Map<String, String>> {
        val dbOk = runCatching {
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { }
                }
            }
        }.isSuccess
        return if (!dbOk) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("status" to "degraded", "database" to "down"))
        } else {
            ResponseEntity.ok(mapOf("status" to "ok", "database" to "up"))
        }
    }
}
