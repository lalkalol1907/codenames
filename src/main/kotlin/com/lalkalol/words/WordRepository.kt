package com.lalkalol.words

import com.lalkalol.common.model.Language
import com.lalkalol.db.jpa.WordJpaRepository
import org.springframework.stereotype.Repository

@Repository
class WordRepository(
    private val wordJpa: WordJpaRepository,
) {
    fun pickRandomWords(language: Language, count: Int): List<String> =
        wordJpa.pickRandomWords(language.code, count).map { it.text }
}
