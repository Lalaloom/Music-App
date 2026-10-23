package com.cameronbatten.dadmusic.setlist.sharing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedSetlistCodecTest {

    @Test
    fun basic_setlist_roundtrips() {
        val sl = SharedSetlist(
            name = "Set A",
            songs = listOf(
                SharedSetlistSong("Hotel California", "Eagles"),
                SharedSetlistSong("Wonderwall", "Oasis"),
            ),
        )
        val decoded = SharedSetlistCodec.decode(SharedSetlistCodec.encode(sl))
        assertEquals(sl, decoded)
    }

    @Test
    fun missing_artist_is_preserved_as_null() {
        val sl = SharedSetlist("Set B", listOf(SharedSetlistSong("Unknown", null)))
        val decoded = SharedSetlistCodec.decode(SharedSetlistCodec.encode(sl))
        assertEquals(sl, decoded)
    }

    @Test
    fun titles_with_quotes_and_newlines_roundtrip() {
        val sl = SharedSetlist(
            name = "Weird \"Set\"",
            songs = listOf(SharedSetlistSong("A \"song\" with\nnewline", "Band & Co")),
        )
        val decoded = SharedSetlistCodec.decode(SharedSetlistCodec.encode(sl))
        assertEquals(sl, decoded)
    }

    @Test
    fun empty_setlist_still_encodes() {
        val sl = SharedSetlist("Empty", emptyList())
        val decoded = SharedSetlistCodec.decode(SharedSetlistCodec.encode(sl))
        assertEquals(sl, decoded)
    }

    @Test
    fun malformed_json_returns_null() {
        assertNull(SharedSetlistCodec.decode("not json"))
        assertNull(SharedSetlistCodec.decode(""))
        assertNull(SharedSetlistCodec.decode("""{"songs":[]}"""))
    }
}
