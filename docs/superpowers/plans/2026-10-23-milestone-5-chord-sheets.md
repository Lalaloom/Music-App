# Milestone 5: Chord sheets + transposition

**Goal:** Scan ChordPro files out of the user's folders, index them in the library, display them with syntax highlighting, and let the user transpose up/down by semitones. Replaces the "separate chord-sheet app on stage" pain point.

**Architecture:** New `ChordSheet` entity in a new table — chord sheets are *documents*, not playback files, so they don't share the `songs` table or `FileType` enum. One migration (v4 → v5) adds the table. `ChordProParser` is a pure Kotlin function. `Transposer` is a pure Kotlin function. The viewer screen renders a monospace column of (chord, lyric) line pairs; transpose controls sit in the top app bar.

**Spec:** §6 v1.1 (chord sheets, transposition).

**Review Focus:**
1. **ChordPro parser handles both chord-only lines and chord-inline lines** (`[C]Hello [G]world` and `C G | Am F` style).
2. **Transposition handles enharmonic preferences** — transposing C up 1 semitone picks Db (not C#) in flat keys; up from G picks G# (not Ab) in sharp keys.
3. **Scanner picks up `.cho` / `.chordpro` / `.pro` / `.crd` extensions** and does NOT confuse them with `.lrc` sidecars.

**Tasks:**

- [ ] **1** — DB: add `ChordSheet` entity + DAO + `MIGRATION_4_5` + bump `AppDatabase.version` to 5.
- [ ] **2** — Scanner: extend `FileScanner.ALLOWED_EXTENSIONS` and `LibraryImporter` to insert ChordSheet rows for `.cho` / `.chordpro` / `.pro` / `.crd`.
- [ ] **3** — Parser: `ChordProParser.parse(text) -> ChordSheetDocument` (directives + list of ChordLine where each ChordLine has segments of (chord?, lyric)).
- [ ] **4** — Transposer: `Transposer.transpose(chord, semitones) -> String` with enharmonic preference.
- [ ] **5** — UI: `ChordSheetViewerScreen` with semitone +/- buttons and the rendered sheet.
- [ ] **6** — Nav + navigator: a new "Chord sheets" entry list on the navigator (reuses NavigatorScreen split). Route `chord/{id}`.
- [ ] **7** — Tests: parser (3 inputs), transposer (semitone wrap + flat/sharp selection).
- [ ] **8** — Smoke doc, PR, merge.
