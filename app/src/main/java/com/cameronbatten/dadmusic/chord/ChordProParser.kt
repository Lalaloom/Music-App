package com.cameronbatten.dadmusic.chord

/**
 * One line of a parsed chord sheet: an alternating sequence of (optional chord)
 * before (literal lyric fragment). An empty-chord segment just prints lyrics;
 * an empty-lyric segment prints a floating chord (useful for intros).
 */
data class ChordSegment(val chord: String?, val lyrics: String)

sealed interface ChordLine {
    /** Blank separator line in the source. */
    data object Blank : ChordLine
    /** A `# comment` line or a `{directive: value}` directive — rendered as text. */
    data class Comment(val text: String) : ChordLine
    /** Normal content line, chords inline (`[C]Hello [G]world`) or chord-only. */
    data class Content(val segments: List<ChordSegment>) : ChordLine
}

data class ChordSheetDocument(
    val title: String?,
    val artist: String?,
    val lines: List<ChordLine>,
)

/**
 * Minimal ChordPro parser:
 * - `{title: ...}`, `{t: ...}` set the document title.
 * - `{artist: ...}`, `{subtitle: ...}`, `{st: ...}` set the document artist.
 * - `# comment` lines pass through as [ChordLine.Comment].
 * - Any other line is split on `[CHORD]` markers into [ChordSegment]s.
 */
object ChordProParser {
    private val directiveRegex = Regex("""\{\s*([a-zA-Z_]+)\s*:\s*(.*?)\s*}""")
    private val chordRegex = Regex("""\[([^\[\]]+)]""")

    fun parse(text: String): ChordSheetDocument {
        var title: String? = null
        var artist: String? = null
        val lines = mutableListOf<ChordLine>()

        for (raw in text.lines()) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                lines += ChordLine.Blank
                continue
            }
            if (trimmed.startsWith("#")) {
                lines += ChordLine.Comment(trimmed.removePrefix("#").trim())
                continue
            }
            val directive = directiveRegex.matchEntire(trimmed)
            if (directive != null) {
                val key = directive.groupValues[1].lowercase()
                val value = directive.groupValues[2]
                when (key) {
                    "title", "t" -> if (title == null) title = value
                    "artist", "subtitle", "st" -> if (artist == null) artist = value
                    else -> lines += ChordLine.Comment(trimmed)
                }
                continue
            }
            lines += ChordLine.Content(parseInlineChords(raw))
        }

        return ChordSheetDocument(title = title, artist = artist, lines = lines)
    }

    private fun parseInlineChords(line: String): List<ChordSegment> {
        val matches = chordRegex.findAll(line).toList()
        if (matches.isEmpty()) {
            return listOf(ChordSegment(chord = null, lyrics = line))
        }
        val segments = mutableListOf<ChordSegment>()
        var cursor = 0
        // Pre-chord prefix (lyrics only, no chord above).
        if (matches.first().range.first > 0) {
            segments += ChordSegment(chord = null, lyrics = line.substring(0, matches.first().range.first))
            cursor = matches.first().range.first
        }
        for ((idx, m) in matches.withIndex()) {
            val chord = m.groupValues[1]
            val lyricsStart = m.range.last + 1
            val lyricsEnd = matches.getOrNull(idx + 1)?.range?.first ?: line.length
            segments += ChordSegment(
                chord = chord,
                lyrics = line.substring(lyricsStart, lyricsEnd),
            )
            cursor = lyricsEnd
        }
        return segments
    }
}
