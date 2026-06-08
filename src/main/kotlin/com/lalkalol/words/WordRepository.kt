package com.lalkalol.words

import com.lalkalol.db.dbQuery
import com.lalkalol.db.tables.WordsTable
import com.lalkalol.game.model.Language
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class WordSeeder {
    suspend fun syncWords() {
        dbQuery {
            syncLanguage(Language.RU, "words/ru.txt")
            syncLanguage(Language.EN, "words/en.txt")
        }
    }

    private fun syncLanguage(language: Language, resourcePath: String) {
        val fileWords = readWords(resourcePath)
        val existing = WordsTable.selectAll()
            .where { WordsTable.language eq language.code }
            .map { it[WordsTable.text] }

        if (existing.sorted() == fileWords.sorted()) {
            return
        }

        WordsTable.deleteWhere { WordsTable.language eq language.code }
        fileWords.forEach { word ->
            WordsTable.insert {
                it[WordsTable.language] = language.code
                it[WordsTable.text] = word
            }
        }
    }

    private fun readWords(resourcePath: String): List<String> {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: error("Word list not found: $resourcePath")
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
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
