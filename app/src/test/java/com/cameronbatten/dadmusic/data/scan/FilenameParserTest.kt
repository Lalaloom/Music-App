package com.cameronbatten.dadmusic.data.scan

import org.junit.Test
import org.junit.Assert.assertEquals

class FilenameParserTest {

    @Test
    fun testHotelCaliforniaEagles() {
        val result = FilenameParser.parse("Hotel California - Eagles.mp3")
        assertEquals("Hotel California", result.title)
        assertEquals("Eagles", result.artist)
    }

    @Test
    fun testEaglesHotelCalifornia() {
        val result = FilenameParser.parse("Eagles - Hotel California.mp3")
        assertEquals("Eagles", result.title)
        assertEquals("Hotel California", result.artist)
    }

    @Test
    fun testTrack13NoArtist() {
        val result = FilenameParser.parse("Track 13.mp3")
        assertEquals("Track 13", result.title)
        assertEquals(null, result.artist)
    }

    @Test
    fun testNoExtension() {
        val result = FilenameParser.parse("NoExtension")
        assertEquals("NoExtension", result.title)
        assertEquals(null, result.artist)
    }

    @Test
    fun testSingleWord() {
        val result = FilenameParser.parse("Single Word.mp3")
        assertEquals("Single Word", result.title)
        assertEquals(null, result.artist)
    }

    @Test
    fun testPaddedSpaces() {
        val result = FilenameParser.parse("  Padded  -  Spaces  .mp3")
        assertEquals("Padded", result.title)
        assertEquals("Spaces", result.artist)
    }

    @Test
    fun testEmptyString() {
        val result = FilenameParser.parse("")
        assertEquals(null, result.title)
        assertEquals(null, result.artist)
    }
}
