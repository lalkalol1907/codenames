package com.lalkalol.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.ApplicationEnvironment
import org.jetbrains.exposed.sql.Database

class DatabaseFactory(environment: ApplicationEnvironment) {
    private val dataSource: HikariDataSource

    val database: Database

    init {
        val config = environment.config.config("database")
        val hikariConfig = HikariConfig().apply {
            driverClassName = config.property("driver").getString()
            jdbcUrl = config.property("url").getString()
            username = config.property("user").getString()
            password = config.property("password").getString()
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        dataSource = HikariDataSource(hikariConfig)
        database = Database.connect(dataSource)
    }
}
