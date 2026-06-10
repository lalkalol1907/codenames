package com.lalkalol.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "rooms")
class RoomEntity(
    @Id
    val id: UUID,
    @Column(length = 4, nullable = false, unique = true)
    val code: String,
    @Column(length = 8, nullable = false)
    val language: String,
    @Column(name = "host_player_id", nullable = false)
    var hostPlayerId: UUID,
    @Column(length = 16, nullable = false)
    var status: String,
    @Column(name = "discord_instance_id", length = 64, unique = true)
    val discordInstanceId: String? = null,
    @Column(name = "discord_channel_id", length = 64)
    val discordChannelId: String? = null,
)
