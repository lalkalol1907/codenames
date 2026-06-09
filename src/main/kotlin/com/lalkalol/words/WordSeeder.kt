package com.lalkalol.words

import com.lalkalol.common.model.Language
import com.lalkalol.db.entity.WordEntity
import com.lalkalol.db.jpa.WordJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WordSeeder(
    private val wordJpa: WordJpaRepository,
) {
    @Transactional
    fun syncWords() {
        syncLanguage(Language.RU, "words/ru.txt")
        syncLanguage(Language.EN, "words/en.txt")
    }

    private fun syncLanguage(language: Language, resourcePath: String) {
        val fileWords = readWords(resourcePath)
        val existing = wordJpa.findAllByLanguage(language.code).map { it.text }

        if (existing.sorted() == fileWords.sorted()) {
            return
        }

        wordJpa.deleteAllByLanguage(language.code)
        wordJpa.saveAll(
            fileWords.map { word ->
                WordEntity(language = language.code, text = word)
            },
        )
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
