package com.lalkalol.words

import com.lalkalol.db.dbQuery
import com.lalkalol.db.tables.WordsTable
import com.lalkalol.game.model.Language
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class WordSeeder {
    suspend fun seedIfEmpty() {
        dbQuery {
            if (WordsTable.selectAll().count() == 0L) {
                seedLanguage(Language.RU, "words/ru.txt")
                seedLanguage(Language.EN, "words/en.txt")
            }
        }
    }

    private fun seedLanguage(language: Language, resourcePath: String) {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: error("Word list not found: $resourcePath")
        stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { word ->
                    WordsTable.insert {
                        it[WordsTable.language] = language.code
                        it[WordsTable.text] = word
                    }
                }
        }
    }
}

class WordRepository {
    suspend fun pickRandomWords(language: Language, count: Int): List<String> = dbQuery {
        WordsTable.selectAll()
            .where { WordsTable.language eq language.code }
            .orderBy(Random())
            .limit(count)
            .map { it[WordsTable.text] }
    }
}
