package com.cameronbatten.dadmusic.chord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordProParserTest {

    @Test
    fun parses_title_and_artist_directives() {
        val doc = ChordProParser.parse(
            """
            {title: Hotel California}
            {artist: Eagles}

            On a dark desert highway
            """.trimIndent(),
        )
        assertEquals("Hotel California", doc.title)
        assertEquals("Eagles", doc.artist)
    }

    @Test
    fun short_directive_aliases_also_work() {
        val doc = ChordProParser.parse(
            """
            {t: Shortcut}
            {st: Band}
            """.trimIndent(),
        )
        assertEquals("Shortcut", doc.title)
        assertEquals("Band", doc.artist)
    }

    @Test
    fun no_directive_leaves_title_null() {
        val doc = ChordProParser.parse("Just lyrics with no directives")
        assertNull(doc.title)
        assertNull(doc.artist)
    }

    @Test
    fun inline_chord_markers_split_segments() {
        val doc = ChordProParser.parse("[C]Hello [G]world")
        val content = doc.lines.first() as ChordLine.Content
        assertEquals(2, content.segments.size)
        assertEquals("C", content.segments[0].chord)
        assertEquals("Hello ", content.segments[0].lyrics)
        assertEquals("G", content.segments[1].chord)
        assertEquals("world", content.segments[1].lyrics)
    }

    @Test
    fun lyrics_before_first_chord_become_chordless_prefix() {
        val doc = ChordProParser.parse("Verse one [Am]starts here")
        val content = doc.lines.first() as ChordLine.Content
        assertNull(content.segments[0].chord)
        assertEquals("Verse one ", content.segments[0].lyrics)
        assertEquals("Am", content.segments[1].chord)
    }

    @Test
    fun blank_lines_are_preserved() {
        val doc = ChordProParser.parse("line one\n\nline two")
        assertTrue(doc.lines[1] is ChordLine.Blank)
    }

    @Test
    fun hash_comment_passes_through_as_comment_line() {
        val doc = ChordProParser.parse("# this is a note")
        val line = doc.lines.first() as ChordLine.Comment
        assertEquals("this is a note", line.text)
    }
}
