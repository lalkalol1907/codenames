package com.lalkalol.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "players")
class PlayerEntity(
    @Id
    val id: UUID,
    @Column(name = "room_id", nullable = false)
    val roomId: UUID,
    @Column(length = 64, nullable = false)
    val name: String,
    @Column(length = 8)
    var team: String?,
    @Column(length = 16)
    var role: String?,
    @Column(name = "is_host", nullable = false)
    var isHost: Boolean,
)
