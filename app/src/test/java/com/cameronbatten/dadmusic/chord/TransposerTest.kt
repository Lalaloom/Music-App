package com.cameronbatten.dadmusic.chord

import org.junit.Assert.assertEquals
import org.junit.Test

class TransposerTest {

    @Test
    fun zero_semitones_is_identity() {
        assertEquals("C", Transposer.transpose("C", 0))
        assertEquals("Dm7", Transposer.transpose("Dm7", 0))
    }

    @Test
    fun positive_shift_uses_sharps() {
        assertEquals("C#", Transposer.transpose("C", 1))
        assertEquals("D#", Transposer.transpose("D", 1))
    }

    @Test
    fun negative_shift_uses_flats() {
        assertEquals("B", Transposer.transpose("C", -1))
        // Shift C down 2 => Bb (flat preference for negative shifts)
        assertEquals("Bb", Transposer.transpose("C", -2))
    }

    @Test
    fun quality_and_extension_are_preserved() {
        assertEquals("C#maj7", Transposer.transpose("Cmaj7", 1))
        assertEquals("D#m7b5", Transposer.transpose("Dm7b5", 1))
    }

    @Test
    fun bass_notes_shift_too() {
        assertEquals("D/F#", Transposer.transpose("C/E", 2))
    }

    @Test
    fun wraps_around_at_octave() {
        assertEquals("C", Transposer.transpose("C", 12))
        assertEquals("C", Transposer.transpose("C", -12))
        assertEquals("D", Transposer.transpose("C", 14))
    }

    @Test
    fun non_chord_strings_pass_through() {
        assertEquals("N.C.", Transposer.transpose("N.C.", 2))
        assertEquals("", Transposer.transpose("", 3))
    }

    @Test
    fun two_char_root_detected() {
        assertEquals("B", Transposer.transpose("Bb", 1))
        assertEquals("D", Transposer.transpose("C#", 1))
    }
}
