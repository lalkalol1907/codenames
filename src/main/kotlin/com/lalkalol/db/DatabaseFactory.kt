package com.lalkalol.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database

class DatabaseFactory(environment: ApplicationEnvironment) : AutoCloseable {
    private val dataSource: HikariDataSource

    val database: Database

    init {
        val config = environment.config.config("database")
        val hikariConfig = HikariConfig().apply {
            driverClassName = config.resolve("driver", "DATABASE_DRIVER")
            jdbcUrl = config.resolve("url", "DATABASE_URL")
            username = config.resolve("user", "DATABASE_USER")
            password = config.resolve("password", "DATABASE_PASSWORD")
            maximumPoolSize = config.propertyOrNull("poolSize")?.getString()?.toIntOrNull() ?: 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        dataSource = HikariDataSource(hikariConfig)
        database = Database.connect(dataSource)
    }

    fun dataSource(): HikariDataSource = dataSource

    override fun close() {
        dataSource.close()
    }

    private fun ApplicationConfig.resolve(key: String, envKey: String): String =
        System.getenv(envKey)?.takeIf { it.isNotBlank() }
            ?: property(key).getString()
}
