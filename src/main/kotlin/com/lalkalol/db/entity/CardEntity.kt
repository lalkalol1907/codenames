package com.lalkalol.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "cards")
class CardEntity(
    @Id
    val id: UUID,
    @Column(name = "game_id", nullable = false)
    val gameId: UUID,
    @Column(nullable = false)
    val position: Int,
    @Column(length = 128, nullable = false)
    val word: String,
    @Column(name = "card_type", length = 16, nullable = false)
    var cardType: String,
    @Column(nullable = false)
    var revealed: Boolean,
)
