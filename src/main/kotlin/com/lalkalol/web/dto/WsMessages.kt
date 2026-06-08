package com.lalkalol.web.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WsClientMessage {
    @Serializable
    @SerialName("give_clue")
    data class GiveClue(
        val word: String,
        val count: Int,
    ) : WsClientMessage

    @Serializable
    @SerialName("guess")
    data class Guess(
        val index: Int,
    ) : WsClientMessage

    @Serializable
    @SerialName("end_turn")
    data object EndTurn : WsClientMessage
}
