package com.cameronbatten.dadmusic.chord

/**
 * Pure chord transposition by semitones.
 *
 * - Preserves the chord quality / extension / bass note. `Cmaj7/G` → `C#maj7/G#`.
 * - Rotates by 12 to normalise negative or oversize shifts.
 * - Enharmonic choice: positive shifts prefer sharps; negative shifts prefer
 *   flats. Simple heuristic matches hand-written sheet-music convention.
 */
object Transposer {
    private val SHARP = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLAT = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    private val NOTE_INDEX = buildMap<String, Int> {
        SHARP.forEachIndexed { i, n -> put(n, i) }
        FLAT.forEachIndexed { i, n -> put(n, i) }
    }

    /**
     * Returns [chord] shifted by [semitones]. Non-chord inputs (e.g. `N.C.`,
     * empty, or anything whose first token isn't a valid note) pass through
     * unchanged.
     */
    fun transpose(chord: String, semitones: Int): String {
        if (chord.isEmpty()) return chord
        val mod = ((semitones % 12) + 12) % 12
        if (mod == 0) return chord

        // Split off a bass note after a slash — `Cmaj7/E`.
        val slash = chord.indexOf('/')
        if (slash > 0) {
            val head = transpose(chord.substring(0, slash), semitones)
            val bass = transpose(chord.substring(slash + 1), semitones)
            return "$head/$bass"
        }

        // Pull the root (1 or 2 chars) then keep the quality.
        val root = detectRoot(chord) ?: return chord
        val quality = chord.substring(root.length)
        val rootIdx = NOTE_INDEX[root] ?: return chord
        val newIdx = ((rootIdx + mod) % 12 + 12) % 12
        val palette = if (semitones >= 0) SHARP else FLAT
        return palette[newIdx] + quality
    }

    private fun detectRoot(chord: String): String? {
        // Two-char candidate first (`Bb`, `C#`), then single-char.
        if (chord.length >= 2) {
            val two = chord.substring(0, 2)
            if (two in NOTE_INDEX) return two
        }
        val one = chord.substring(0, 1)
        return if (one in NOTE_INDEX) one else null
    }
}
