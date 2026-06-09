package com.lalkalol.db.jpa

import com.lalkalol.db.entity.GameEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GameJpaRepository : JpaRepository<GameEntity, UUID> {
    fun findByRoomId(roomId: UUID): GameEntity?

    fun deleteByRoomId(roomId: UUID)
}
