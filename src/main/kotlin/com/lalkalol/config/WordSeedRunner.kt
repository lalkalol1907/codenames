package com.lalkalol.config

import com.lalkalol.words.WordSeeder
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class WordSeedRunner(
    private val wordSeeder: WordSeeder,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        wordSeeder.syncWords()
    }
}
