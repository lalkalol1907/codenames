package com.lalkalol.config

import com.lalkalol.db.DatabaseFactory
import com.lalkalol.db.tables.CardsTable
import com.lalkalol.db.tables.GamesTable
import com.lalkalol.db.tables.PlayersTable
import com.lalkalol.db.tables.RoomsTable
import com.lalkalol.db.tables.WordsTable
import com.lalkalol.words.WordSeeder
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    runBlocking {
        val factory = dependencies.resolve<DatabaseFactory>()
        transaction(factory.database) {
            SchemaUtils.create(
                WordsTable,
                RoomsTable,
                PlayersTable,
                GamesTable,
                CardsTable,
            )
        }
        dependencies.resolve<WordSeeder>().seedIfEmpty()
    }
}
