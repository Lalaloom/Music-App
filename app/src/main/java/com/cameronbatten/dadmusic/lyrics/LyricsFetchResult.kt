package com.cameronbatten.dadmusic.lyrics

/**
 * Outcome of a single lyric-fetch attempt against LRCLIB.
 *
 * The three cases map one-to-one with the terminal states the UI needs to show
 * — the Zen view distinguishes "we have lyrics, synced?" ([Fetched]), "LRCLIB
 * confirmed there are no lyrics" ([NotFound]) and "we couldn't talk to LRCLIB"
 * ([OfflineOrFailed]). Everything deeper (HTTP code, JSON parse errors, read
 * timeouts) rolls into the latter.
 */
sealed interface LyricsFetchResult {
    data class Fetched(val hasTimings: Boolean) : LyricsFetchResult
    data object NotFound : LyricsFetchResult
    data class OfflineOrFailed(val reason: String) : LyricsFetchResult
}
