# Milestone 10: Shared setlists (export / import)

**Goal:** Ship a `.setlist.json` interchange format so musicians can send their setlist to each other without a cloud account.

**Architecture:** Hand-rolled JSON codec (no external library — same style as M9 wire format) encodes `{ name, songs: [{title, artist?}, …] }`. Export flows into `ACTION_CREATE_DOCUMENT`; import uses `ACTION_OPEN_DOCUMENT` + matches by `(title, artist)` case-insensitive against the local `SongDao`. Unmatched songs are listed in a dialog.

**Spec:** §6 v2 (Shared setlists — export / import between musicians).

**Tasks:**
- `SharedSetlist` / `SharedSetlistSong` + `SharedSetlistCodec` (encode / decode).
- `SetlistExporter` pulls setlist + entries → JSON string.
- `SetlistImporter` parses JSON, looks songs up by `(title.lowercase(), artist?.lowercase())`, creates a fresh `Setlist` with the matched entries, reports a `SetlistImportResult`.
- `SetlistSharingViewModel` + `SetlistSharingScreen` with export per-row + import button + "Missing" dialog.
- Settings has a "Shared setlists" section with a CTA; nav route `share-setlists`.
- JVM test suite for the codec (roundtrip, nulls, quoted / newlined titles, empty, malformed).

Manual smoke in `docs/superpowers/manual-smoke-M10.md`.
