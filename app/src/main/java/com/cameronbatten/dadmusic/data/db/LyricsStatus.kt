package com.cameronbatten.dadmusic.data.db

/**
 * Lifecycle of a song's lyrics attachment, stored as a column on [Song].
 *
 * - [MISSING]              — nothing on disk or in cache; eligible for LRCLIB fetch.
 * - [FETCHING]             — a bulk or per-song fetch is in flight.
 * - [ADOPTED_FROM_DISK]    — a `.lrc` sidecar sat next to the music file during scan and
 *                            was adopted verbatim; LRCLIB fetch is skipped (spec §5.1).
 * - [FETCHED_SYNCED]       — LRCLIB returned a time-synced LRC file; cached locally.
 * - [FETCHED_UNSYNCED]     — LRCLIB returned plain lyrics only; cached locally.
 *                            Zen view falls back to linear scroll (spec §5.2).
 * - [FAILED]               — fetch attempted and failed (offline, 404, parse error).
 *                            User can retry via Settings → Re-fetch all lyrics.
 */
enum class LyricsStatus {
    MISSING,
    FETCHING,
    ADOPTED_FROM_DISK,
    FETCHED_SYNCED,
    FETCHED_UNSYNCED,
    FAILED,
}
