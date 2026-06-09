package com.lalkalol.db

import com.lalkalol.testsupport.h2TestJdbcUrl
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MigrationSmokeTest {
    @Test
    fun `flyway schema works on h2`() {
        val jdbcUrl = h2TestJdbcUrl("migration-smoke-${System.nanoTime()}")
        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration")
            .load()
        flyway.migrate()

        java.sql.DriverManager.getConnection(jdbcUrl, "sa", "").use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'words' ORDER BY ORDINAL_POSITION",
                )
                val columns = buildList {
                    while (rs.next()) add(rs.getString(1))
                }
                assertTrue(columns.contains("language"), "columns=$columns")
            }
            conn.prepareStatement("INSERT INTO words (language, text) VALUES (?, ?)").use { ps ->
                ps.setString(1, "ru")
                ps.setString(2, "word")
                ps.executeUpdate()
            }
        }
    }
}
