package com.lalkalol.db.jpa

import com.lalkalol.db.entity.RoomEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface RoomJpaRepository : JpaRepository<RoomEntity, UUID> {
    fun findByCode(code: String): RoomEntity?

    fun existsByCode(code: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.code = :code")
    fun findByCodeForUpdate(@Param("code") code: String): RoomEntity?

    fun findByDiscordInstanceId(discordInstanceId: String): RoomEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.discordInstanceId = :instanceId")
    fun findByDiscordInstanceIdForUpdate(@Param("instanceId") instanceId: String): RoomEntity?

    fun findByStatusInAndCreatedAtBefore(statuses: List<String>, cutoff: LocalDateTime): List<RoomEntity>
}
