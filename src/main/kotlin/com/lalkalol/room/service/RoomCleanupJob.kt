package com.lalkalol.room.service

import com.lalkalol.config.AppProperties
import com.lalkalol.db.jpa.CardJpaRepository
import com.lalkalol.db.jpa.GameJpaRepository
import com.lalkalol.db.jpa.PlayerJpaRepository
import com.lalkalol.db.jpa.RoomJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class RoomCleanupJob(
    private val roomJpa: RoomJpaRepository,
    private val playerJpa: PlayerJpaRepository,
    private val gameJpa: GameJpaRepository,
    private val cardJpa: CardJpaRepository,
    private val appProperties: AppProperties,
) {
    private val log = LoggerFactory.getLogger(RoomCleanupJob::class.java)

    @Scheduled(cron = "\${app.cleanup.cron:0 0 3 * * *}")
    @Transactional
    fun cleanupOldRooms() {
        if (!appProperties.cleanup.enabled) return

        val cutoff = LocalDateTime.now().minusDays(appProperties.cleanup.roomRetentionDays)
        val oldRooms = roomJpa.findByStatusInAndCreatedAtBefore(
            listOf("LOBBY", "FINISHED"),
            cutoff,
        )

        if (oldRooms.isEmpty()) {
            log.debug("Room cleanup: nothing to delete (cutoff={})", cutoff)
            return
        }

        val roomIds = oldRooms.map { it.id }
        val games = gameJpa.findByRoomIdIn(roomIds)

        for (game in games) {
            cardJpa.deleteAllByGameId(game.id)
        }
        gameJpa.deleteAll(games)

        for (roomId in roomIds) {
            playerJpa.deleteAllByRoomId(roomId)
        }
        roomJpa.deleteAll(oldRooms)

        log.info("Room cleanup: deleted {} rooms older than {} days", oldRooms.size, appProperties.cleanup.roomRetentionDays)
    }
}
