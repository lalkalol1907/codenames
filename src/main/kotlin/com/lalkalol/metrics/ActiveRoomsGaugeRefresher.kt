package com.lalkalol.metrics

import com.lalkalol.common.model.RoomStatus
import com.lalkalol.db.jpa.RoomJpaRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Periodically counts rooms by status and updates the Micrometer gauge values.
 * Runs every 30 seconds — cheap COUNT query is fine at this frequency.
 */
@Component
class ActiveRoomsGaugeRefresher(
    private val roomJpa: RoomJpaRepository,
    private val metricsService: MetricsService,
) {
    @Scheduled(fixedDelay = 30_000)
    fun refresh() {
        val all = roomJpa.findAll()
        val lobby = all.count { it.status == RoomStatus.LOBBY.name }
        val playing = all.count { it.status == RoomStatus.PLAYING.name }
        metricsService.setActiveRoomsLobby(lobby.toLong())
        metricsService.setActiveRoomsPlaying(playing.toLong())
    }
}
