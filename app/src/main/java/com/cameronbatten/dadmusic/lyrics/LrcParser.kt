package com.cameronbatten.dadmusic.lyrics

/**
 * One entry in a parsed LRC file.
 *
 * @param startMs Milliseconds from song start when this line becomes "current".
 *                `-1L` is a sentinel value for *unsynced* lines; see [LrcParser].
 * @param text    The lyric text, trimmed. May be empty when the LRC file encodes an
 *                intentional silence gap as `[mm:ss.xx]` with no text after it.
 */
data class TimedLine(val startMs: Long, val text: String) {
    val isSynced: Boolean get() = startMs >= 0L
}

/**
 * Pure-Kotlin LRC parser.
 *
 * ### Supported syntax
 * - `[mm:ss]text`           — second precision
 * - `[mm:ss.xx]text`        — centisecond precision (most common)
 * - `[mm:ss.xxx]text`       — millisecond precision
 * - `[mm:ss:xx]text`        — alternate separator (some older tools emit this)
 * - `[mm:ss][mm:ss]text`    — repeated timestamp on one line; emits one entry per timestamp
 * - `[mm:ss]`               — empty-text timestamp (intentional silence gap)
 *
 * ### Metadata tags
 * `[ar:...]`, `[ti:...]`, `[length:...]`, etc. do not match the digit-only timestamp
 * grammar and are dropped. They are preserved verbatim inside [Lyrics.rawLrc] on disk.
 *
 * ### Unsynced input
 * If **no** line in the input contains a timestamp, every non-blank line is returned
 * as a [TimedLine] with `startMs = -1L` and text trimmed. Blank lines are preserved
 * as `TimedLine(-1L, "")` because the Zen view distributes lines evenly across the
 * song duration and treats blanks as half-weight gaps (spec §5.2).
 *
 * ### Malformed timestamps
 * A line that *looks* like a tag but doesn't match the digit grammar (e.g. `[00:xx.yy]`)
 * is dropped silently — we never crash on bad input, we just move on.
 *
 * ### Ordering
 * Results are sorted by `startMs` ascending. [kotlin.collections.sortedBy] is a stable
 * sort, so repeated timestamps keep their insertion order.
 */
object LrcParser {

    // mm can exceed 60 for long songs. Fractional group is optional and 1–3 digits.
    private val TIMESTAMP_RE = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(lrcText: String): List<TimedLine> {
        val lines = lrcText.lines()
        val anyTimestamped = lines.any { TIMESTAMP_RE.containsMatchIn(it) }

        if (!anyTimestamped) {
            return lines.map { TimedLine(startMs = -1L, text = it.trim()) }
        }

        val results = mutableListOf<TimedLine>()
        for (line in lines) {
            val matches = TIMESTAMP_RE.findAll(line).toList()
            if (matches.isEmpty()) continue // metadata header or malformed line

            val lastEnd = matches.last().range.last + 1
            val text = if (lastEnd < line.length) line.substring(lastEnd).trim() else ""

            for (match in matches) {
                val mm = match.groupValues[1].toLongOrNull() ?: continue
                val ss = match.groupValues[2].toLongOrNull() ?: continue
                val fracStr = match.groupValues[3]
                val frac = when (fracStr.length) {
                    0 -> 0L
                    1 -> fracStr.toLong() * 100
                    2 -> fracStr.toLong() * 10
                    3 -> fracStr.toLong()
                    else -> fracStr.take(3).toLong()
                }
                val startMs = (mm * 60L + ss) * 1000L + frac
                results.add(TimedLine(startMs = startMs, text = text))
            }
        }
        return results.sortedBy { it.startMs }
    }
}
