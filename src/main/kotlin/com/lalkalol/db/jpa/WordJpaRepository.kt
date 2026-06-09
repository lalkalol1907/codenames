package com.lalkalol.db.jpa

import com.lalkalol.db.entity.WordEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WordJpaRepository : JpaRepository<WordEntity, Long> {
    fun findAllByLanguage(language: String): List<WordEntity>

    fun deleteAllByLanguage(language: String)

    @Query(
        value = "SELECT * FROM words WHERE language = :language ORDER BY RANDOM() LIMIT :count",
        nativeQuery = true,
    )
    fun pickRandomWords(
        @Param("language") language: String,
        @Param("count") count: Int,
    ): List<WordEntity>
}
