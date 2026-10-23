package com.cameronbatten.dadmusic.pedal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PedalBindingsTest {

    @Test
    fun encode_then_decode_roundtrips() {
        val bindings = listOf(
            PedalBinding(92, PedalAction.SCROLL_UP),
            PedalBinding(93, PedalAction.SCROLL_DOWN),
            PedalBinding(126, PedalAction.PLAY_PAUSE),
        )
        val roundtripped = PedalBindings.decode(PedalBindings.encode(bindings))
        assertEquals(bindings.toSet(), roundtripped.toSet())
    }

    @Test
    fun empty_or_null_text_decodes_to_empty_list() {
        assertTrue(PedalBindings.decode("").isEmpty())
        assertTrue(PedalBindings.decode(null).isEmpty())
        assertTrue(PedalBindings.decode("   ").isEmpty())
    }

    @Test
    fun malformed_entries_are_dropped() {
        val decoded = PedalBindings.decode("92:SCROLL_UP;not-a-pair;93:NOT_AN_ACTION;126:PLAY_PAUSE")
        assertEquals(2, decoded.size)
        assertEquals(PedalAction.SCROLL_UP, decoded.firstOrNull { it.keyCode == 92 }?.action)
        assertEquals(PedalAction.PLAY_PAUSE, decoded.firstOrNull { it.keyCode == 126 }?.action)
    }

    @Test
    fun duplicate_key_codes_last_wins() {
        val decoded = PedalBindings.decode("92:SCROLL_UP;92:PLAY_PAUSE")
        assertEquals(1, decoded.size)
        assertEquals(PedalAction.PLAY_PAUSE, decoded.first().action)
    }

    @Test
    fun actionFor_finds_match_or_returns_null() {
        val bindings = listOf(PedalBinding(92, PedalAction.NEXT))
        assertEquals(PedalAction.NEXT, PedalBindings.actionFor(bindings, 92))
        assertNull(PedalBindings.actionFor(bindings, 999))
    }
}
