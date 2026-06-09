package com.lalkalol.game.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = WsClientMessage.GiveClue::class, name = "give_clue"),
    JsonSubTypes.Type(value = WsClientMessage.Guess::class, name = "guess"),
    JsonSubTypes.Type(value = WsClientMessage.EndTurn::class, name = "end_turn"),
)
sealed interface WsClientMessage {
    data class GiveClue(
        val word: String,
        val count: Int,
    ) : WsClientMessage

    data class Guess(
        val index: Int,
    ) : WsClientMessage

    data object EndTurn : WsClientMessage
}
