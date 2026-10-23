package com.cameronbatten.dadmusic.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parse_standardCentisecondTimestamps() {
        val lrc = """
            [00:12.00]Line one
            [00:15.50]Line two
            [00:18.25]Line three
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(3, result.size)
        assertEquals(TimedLine(12_000L, "Line one"), result[0])
        assertEquals(TimedLine(15_500L, "Line two"), result[1])
        assertEquals(TimedLine(18_250L, "Line three"), result[2])
    }

    @Test
    fun parse_secondPrecisionWithoutFraction() {
        val result = LrcParser.parse("[00:05]hello")
        assertEquals(1, result.size)
        assertEquals(TimedLine(5_000L, "hello"), result[0])
    }

    @Test
    fun parse_millisecondPrecision() {
        val result = LrcParser.parse("[00:05.123]hello")
        assertEquals(TimedLine(5_123L, "hello"), result[0])
    }

    @Test
    fun parse_alternateColonSeparator() {
        val result = LrcParser.parse("[00:05:50]hello")
        assertEquals(TimedLine(5_500L, "hello"), result[0])
    }

    @Test
    fun parse_longSongBeyondOneHour() {
        val result = LrcParser.parse("[123:45.00]late line")
        assertEquals(TimedLine((123L * 60 + 45) * 1000, "late line"), result[0])
    }

    @Test
    fun parse_repeatedTimestampsOnOneLineEmitOnePerTimestamp() {
        val result = LrcParser.parse("[00:10.00][00:50.00]Chorus")
        assertEquals(2, result.size)
        assertEquals(TimedLine(10_000L, "Chorus"), result[0])
        assertEquals(TimedLine(50_000L, "Chorus"), result[1])
    }

    @Test
    fun parse_identicalTimestampsPreserveInsertionOrder() {
        val lrc = """
            [00:10.00]First
            [00:10.00]Second
            [00:10.00]Third
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(listOf("First", "Second", "Third"), result.map { it.text })
    }

    @Test
    fun parse_emptyTextTimestampPreservedAsSilenceGap() {
        val lrc = """
            [00:12.00]Line
            [00:14.00]
            [00:16.00]Next line
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(3, result.size)
        assertEquals(TimedLine(14_000L, ""), result[1])
        assertTrue(result[1].isSynced)
    }

    @Test
    fun parse_metadataHeadersAreDropped() {
        val lrc = """
            [ar:Queen]
            [ti:Bohemian Rhapsody]
            [al:A Night at the Opera]
            [length:05:55]
            [00:00.00]Is this the real life
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        assertEquals("Is this the real life", result[0].text)
    }

    @Test
    fun parse_malformedTimestampSkipsLineButKeepsOthers() {
        val lrc = """
            [00:00.00]Good line
            [00:xx.yy]Bad line
            [00:10.00]Another good line
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(2, result.size)
        assertEquals("Good line", result[0].text)
        assertEquals("Another good line", result[1].text)
    }

    @Test
    fun parse_unsyncedLyricsReturnMinusOneSentinel() {
        val lrc = """
            Verse one line one
            Verse one line two
            Chorus
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(3, result.size)
        result.forEach { assertEquals(-1L, it.startMs) }
        result.forEach { assertFalse(it.isSynced) }
        assertEquals(listOf("Verse one line one", "Verse one line two", "Chorus"), result.map { it.text })
    }

    @Test
    fun parse_unsyncedLyricsPreserveBlankLinesAsEmptyEntries() {
        val lrc = "line one\n\nline two"
        val result = LrcParser.parse(lrc)
        assertEquals(3, result.size)
        assertEquals("line one", result[0].text)
        assertEquals("", result[1].text)
        assertEquals("line two", result[2].text)
    }

    @Test
    fun parse_textWithLeadingAndTrailingWhitespaceIsTrimmed() {
        val result = LrcParser.parse("[00:05.00]   padded   ")
        assertEquals(TimedLine(5_000L, "padded"), result[0])
    }

    @Test
    fun parse_outOfOrderInputIsSortedByTime() {
        val lrc = """
            [00:30.00]Third
            [00:10.00]First
            [00:20.00]Second
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(listOf("First", "Second", "Third"), result.map { it.text })
    }

    @Test
    fun parse_emptyStringReturnsOneEmptyUnsyncedEntry() {
        // Deliberate: an empty string has one line (which is empty), so one entry.
        val result = LrcParser.parse("")
        assertEquals(1, result.size)
        assertEquals(TimedLine(-1L, ""), result[0])
    }
}
