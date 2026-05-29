package com.lalkalol.config

import com.lalkalol.db.DatabaseFactory
import com.lalkalol.game.repository.GameRepository
import com.lalkalol.game.rules.BoardGenerator
import com.lalkalol.game.service.GameService
import com.lalkalol.room.repository.RoomRepository
import com.lalkalol.room.service.RoomService
import com.lalkalol.web.GameSessionHub
import com.lalkalol.words.WordRepository
import com.lalkalol.words.WordSeeder
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve

fun Application.configureDependencyInjection() {
    val appEnvironment = this@configureDependencyInjection.environment
    dependencies {
        provide { DatabaseFactory(appEnvironment) }
        provide { WordSeeder() }
        provide { WordRepository() }
        provide { GameRepository() }
        provide { RoomRepository(resolve()) }
        provide { BoardGenerator(resolve()) }
        provide { GameService(resolve(), resolve(), resolve()) }
        provide { RoomService(resolve(), resolve()) }
        provide { GameSessionHub(resolve(), resolve()) }
    }
}
