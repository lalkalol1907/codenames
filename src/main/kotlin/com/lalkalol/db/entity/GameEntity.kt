package com.lalkalol.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(name = "games")
class GameEntity(
    @Id
    val id: UUID,
    @Column(name = "room_id", nullable = false, unique = true)
    val roomId: UUID,
    @Column(name = "starting_team", length = 8, nullable = false)
    val startingTeam: String,
    @Column(name = "current_team", length = 8, nullable = false)
    var currentTeam: String,
    @Column(length = 16, nullable = false)
    var phase: String,
    @Column(name = "clue_word", length = 64)
    var clueWord: String?,
    @Column(name = "clue_count")
    var clueCount: Int?,
    @Column(name = "guesses_remaining", nullable = false)
    var guessesRemaining: Int,
    @Column(name = "winner_team", length = 8)
    var winnerTeam: String?,
    @Version
    var version: Int = 0,
)
