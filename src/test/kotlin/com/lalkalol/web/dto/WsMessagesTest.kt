package com.lalkalol.web.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WsMessagesTest {
    private val json = wsJson

    @Test
    fun `decodes give clue message`() {
        val msg = json.decodeFromString<WsClientMessage>("""{"type":"give_clue","word":"ocean","count":2}""")
        assertIs<WsClientMessage.GiveClue>(msg)
        assertEquals("ocean", msg.word)
        assertEquals(2, msg.count)
    }

    @Test
    fun `decodes guess message`() {
        val msg = json.decodeFromString<WsClientMessage>("""{"type":"guess","index":12}""")
        assertIs<WsClientMessage.Guess>(msg)
        assertEquals(12, msg.index)
    }

    @Test
    fun `decodes end turn message`() {
        val msg = json.decodeFromString<WsClientMessage>("""{"type":"end_turn"}""")
        assertIs<WsClientMessage.EndTurn>(msg)
    }
}
