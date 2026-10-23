package com.cameronbatten.dadmusic.ui.lyricseditor

import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedLyricsEditorLrcTest {

    @Test
    fun buildLrcText_handles_mix_of_timed_and_untimed_lines() {
        val lines = listOf(
            EditableLine("Intro", null),
            EditableLine("First line", 10_000L),
            EditableLine("Second", 12_500L),
            EditableLine("Untimed again", null),
        )
        val lrc = AdvancedLyricsEditorViewModel.buildLrcText(lines)
        val expected = listOf(
            "Intro",
            "[00:10.00]First line",
            "[00:12.50]Second",
            "Untimed again",
        ).joinToString("\n")
        assertEquals(expected, lrc)
    }

    @Test
    fun buildLrcText_centiseconds_round_down() {
        val lines = listOf(EditableLine("x", 7_899L))
        // 7899 ms → 00:07.89
        val expected = "[00:07.89]x"
        assertEquals(expected, AdvancedLyricsEditorViewModel.buildLrcText(lines))
    }
}
