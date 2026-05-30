package com.lalkalol.db

import com.lalkalol.db.tables.WordsTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertTrue

class MigrationSmokeTest {
    @Test
    fun `flyway schema works with exposed on h2`() {
        val cfg = HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            jdbcUrl = h2TestJdbcUrl("migration-smoke-${System.nanoTime()}")
            username = "sa"
            password = ""
        }
        HikariDataSource(cfg).use { ds ->
            MigrationRunner.migrate(ds)
            ds.connection.use { conn ->
                val rs = conn.createStatement().executeQuery(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'words' ORDER BY ORDINAL_POSITION",
                )
                val columns = buildList {
                    while (rs.next()) add(rs.getString(1))
                }
                assertTrue(columns.contains("language"), "columns=$columns")
            }
            val db = org.jetbrains.exposed.sql.Database.connect(ds)
            transaction(db) {
                WordsTable.insert {
                    it[WordsTable.language] = "ru"
                    it[WordsTable.text] = "word"
                }
            }
        }
    }
}

internal fun h2TestJdbcUrl(name: String): String =
    "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
