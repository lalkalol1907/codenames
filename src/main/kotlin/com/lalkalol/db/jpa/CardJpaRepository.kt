package com.lalkalol.db.jpa

import com.lalkalol.db.entity.CardEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CardJpaRepository : JpaRepository<CardEntity, UUID> {
    fun findAllByGameIdOrderByPositionAsc(gameId: UUID): List<CardEntity>

    fun deleteAllByGameId(gameId: UUID)
}
