package com.lalkalol.room.web

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
@ConditionalOnProperty(
    prefix = "app.scaling",
    name = ["redis-enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class InMemoryPresenceTracker : PresenceTracker {
    private val counts = ConcurrentHashMap<UUID, AtomicInteger>()

    override fun increment(playerId: UUID) {
        counts.computeIfAbsent(playerId) { AtomicInteger(0) }.incrementAndGet()
    }

    override fun decrement(playerId: UUID): Long {
        val counter = counts[playerId] ?: return 0L
        val result = counter.decrementAndGet().toLong()
        if (result <= 0L) counts.remove(playerId)
        return maxOf(result, 0L)
    }

    override fun count(playerId: UUID): Long = counts[playerId]?.get()?.toLong() ?: 0L
}
