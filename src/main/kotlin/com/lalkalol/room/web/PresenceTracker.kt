package com.lalkalol.room.web

import java.util.UUID

interface PresenceTracker {
    fun increment(playerId: UUID)
    fun decrement(playerId: UUID): Long
    fun count(playerId: UUID): Long
}
