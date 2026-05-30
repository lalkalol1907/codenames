package com.lalkalol.config

import com.lalkalol.db.DatabaseFactory
import com.lalkalol.db.MigrationRunner
import com.lalkalol.words.WordSeeder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    runBlocking {
        val factory = dependencies.resolve<DatabaseFactory>()
        MigrationRunner.migrate(factory.dataSource())
        dependencies.resolve<WordSeeder>().seedIfEmpty()
    }

    monitor.subscribe(ApplicationStopping) {
        runBlocking {
            dependencies.resolve<DatabaseFactory>().close()
        }
    }
}

suspend fun Application.checkDatabase(): Boolean = runCatching {
    val factory = dependencies.resolve<DatabaseFactory>()
    transaction(factory.database) {
        exec("SELECT 1")
    }
}.isSuccess
