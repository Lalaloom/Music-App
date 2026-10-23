package com.cameronbatten.dadmusic.band

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BandSyncProtocolTest {

    @Test
    fun play_roundtrips() {
        val msg = BandSyncMessage.Play(songUri = "content://foo/song.mp3", positionMs = 1234L)
        val wire = BandSyncProtocol.encode(msg)
        val decoded = BandSyncProtocol.decode(wire)
        assertEquals(msg, decoded)
    }

    @Test
    fun pause_roundtrips() {
        val msg = BandSyncMessage.Pause(positionMs = 42L)
        assertEquals(msg, BandSyncProtocol.decode(BandSyncProtocol.encode(msg)))
    }

    @Test
    fun heartbeat_roundtrips() {
        val msg = BandSyncMessage.Heartbeat(epochMs = 1_700_000_000_000L)
        assertEquals(msg, BandSyncProtocol.decode(BandSyncProtocol.encode(msg)))
    }

    @Test
    fun uri_with_quotes_escapes_correctly() {
        val tricky = """content://foo?title="Hello"&artist=Eagles"""
        val msg = BandSyncMessage.Play(tricky, 0L)
        val wire = BandSyncProtocol.encode(msg)
        val decoded = BandSyncProtocol.decode(wire)
        assertEquals(msg, decoded)
    }

    @Test
    fun malformed_lines_return_null() {
        assertNull(BandSyncProtocol.decode("not json"))
        assertNull(BandSyncProtocol.decode("""{"kind":"play"}"""))
        assertNull(BandSyncProtocol.decode("""{"kind":"play","songUri":"a"}"""))
        assertNull(BandSyncProtocol.decode("""{"kind":"unknown"}"""))
        assertNull(BandSyncProtocol.decode(""))
    }
}
