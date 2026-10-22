package com.cameronbatten.dadmusic.ui.zen

import com.cameronbatten.dadmusic.lyrics.TimedLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LineIndexComputationTest {

    private fun synced(vararg pairs: Pair<Long, String>): List<TimedLine> =
        pairs.map { TimedLine(it.first, it.second) }

    @Test
    fun emptyLines_returnsMinusOne() {
        assertEquals(-1, computeCurrentLineIndex(emptyList(), 1000, 180_000))
    }

    @Test
    fun beforeFirstTimestamp_returnsZero() {
        val lines = synced(1000L to "a", 2000L to "b")
        assertEquals(0, computeCurrentLineIndex(lines, 500, 180_000))
    }

    @Test
    fun exactlyOnBoundary_advancesToThatLine() {
        val lines = synced(1000L to "a", 2000L to "b", 3000L to "c")
        assertEquals(1, computeCurrentLineIndex(lines, 2000, 180_000))
    }

    @Test
    fun betweenBoundaries_returnsEarlierLine() {
        val lines = synced(1000L to "a", 2000L to "b", 3000L to "c")
        assertEquals(1, computeCurrentLineIndex(lines, 2500, 180_000))
    }

    @Test
    fun pastLastTimestamp_returnsLastLine() {
        val lines = synced(1000L to "a", 2000L to "b")
        assertEquals(1, computeCurrentLineIndex(lines, 10_000, 180_000))
    }

    @Test
    fun unsyncedLines_distributedEvenlyAcrossDuration() {
        val lines = listOf(
            TimedLine(-1L, "a"),
            TimedLine(-1L, "b"),
            TimedLine(-1L, "c"),
            TimedLine(-1L, "d"),
        )
        // duration = 40s, 4 lines, 10s each
        assertEquals(0, computeCurrentLineIndex(lines, 5_000, 40_000))
        assertEquals(1, computeCurrentLineIndex(lines, 15_000, 40_000))
        assertEquals(2, computeCurrentLineIndex(lines, 25_000, 40_000))
        assertEquals(3, computeCurrentLineIndex(lines, 35_000, 40_000))
    }

    @Test
    fun unsyncedLines_withZeroDuration_fallsBackToFirst() {
        val lines = listOf(TimedLine(-1L, "a"), TimedLine(-1L, "b"))
        assertEquals(0, computeCurrentLineIndex(lines, 1_000, 0))
    }

    @Test
    fun offsetShiftsBoundary_onPositive() {
        // Caller already added offset to position; this test confirms the math
        // works the same way the ViewModel wires it.
        val lines = synced(1000L to "a", 2000L to "b")
        val position = 1500L
        val offset = 500L
        // position + offset = 2000 → should jump to index 1
        assertEquals(1, computeCurrentLineIndex(lines, position + offset, 180_000))
    }
}
