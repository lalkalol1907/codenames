package com.lalkalol.db.tables

import org.jetbrains.exposed.sql.Table

object WordsTable : Table("words") {
    val id = long("id").autoIncrement()
    val language = varchar("language", 8)
    val text = varchar("text", 128)

    override val primaryKey = PrimaryKey(id)
}

object RoomsTable : Table("rooms") {
    val id = uuid("id")
    val code = varchar("code", 4).uniqueIndex()
    val language = varchar("language", 8)
    val hostPlayerId = uuid("host_player_id")
    val status = varchar("status", 16)

    override val primaryKey = PrimaryKey(id)
}

object PlayersTable : Table("players") {
    val id = uuid("id")
    val roomId = uuid("room_id").references(RoomsTable.id)
    val name = varchar("name", 64)
    val team = varchar("team", 8).nullable()
    val role = varchar("role", 16).nullable()
    val isHost = bool("is_host")

    override val primaryKey = PrimaryKey(id)
}

object GamesTable : Table("games") {
    val id = uuid("id")
    val roomId = uuid("room_id").references(RoomsTable.id).uniqueIndex()
    val startingTeam = varchar("starting_team", 8)
    val currentTeam = varchar("current_team", 8)
    val phase = varchar("phase", 16)
    val clueWord = varchar("clue_word", 64).nullable()
    val clueCount = integer("clue_count").nullable()
    val guessesRemaining = integer("guesses_remaining")
    val winnerTeam = varchar("winner_team", 8).nullable()
    val version = integer("version")

    override val primaryKey = PrimaryKey(id)
}

object CardsTable : Table("cards") {
    val id = uuid("id")
    val gameId = uuid("game_id").references(GamesTable.id)
    val position = integer("position")
    val word = varchar("word", 128)
    val cardType = varchar("card_type", 16)
    val revealed = bool("revealed")

    override val primaryKey = PrimaryKey(id)
}
