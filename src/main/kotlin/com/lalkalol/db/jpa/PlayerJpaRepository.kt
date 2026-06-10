package com.lalkalol.db.jpa

import com.lalkalol.db.entity.PlayerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerJpaRepository : JpaRepository<PlayerEntity, UUID> {
    fun findAllByRoomId(roomId: UUID): List<PlayerEntity>

    fun deleteAllByRoomId(roomId: UUID)

    fun findByRoomIdAndDiscordUserId(roomId: UUID, discordUserId: String): PlayerEntity?
}
