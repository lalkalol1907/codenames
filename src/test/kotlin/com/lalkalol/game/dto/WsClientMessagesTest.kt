package com.lalkalol.game.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class WsClientMessagesTest {
    private val objectMapper = ObjectMapper().registerModule(kotlinModule())

    @Test
    fun `decodes give clue message`() {
        val msg = objectMapper.readValue(
            """{"type":"give_clue","word":"ocean","count":2}""",
            WsClientMessage::class.java,
        )
        assertInstanceOf(WsClientMessage.GiveClue::class.java, msg)
        msg as WsClientMessage.GiveClue
        assertEquals("ocean", msg.word)
        assertEquals(2, msg.count)
    }

    @Test
    fun `decodes guess message`() {
        val msg = objectMapper.readValue(
            """{"type":"guess","index":12}""",
            WsClientMessage::class.java,
        )
        assertInstanceOf(WsClientMessage.Guess::class.java, msg)
        msg as WsClientMessage.Guess
        assertEquals(12, msg.index)
    }

    @Test
    fun `decodes end turn message`() {
        val msg = objectMapper.readValue(
            """{"type":"end_turn"}""",
            WsClientMessage::class.java,
        )
        assertInstanceOf(WsClientMessage.EndTurn::class.java, msg)
    }
}
