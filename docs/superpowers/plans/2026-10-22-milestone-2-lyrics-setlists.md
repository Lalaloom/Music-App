# Milestone 2: Lyrics + Setlists + Zen Performance View

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a signed APK that turns the M1 song library into a gig-ready instrument: pre-existing `.lrc` files on disk are honored, missing lyrics are bulk-fetched from LRCLIB, a Zen performance view auto-scrolls three lines of time-synced lyrics over mp3 playback, each imported folder becomes a Setlist, the user can switch between setlists in a split navigator, and in-session reorders / request-adds live in a device-local live queue with a 10-second undo on each add.

**Architecture:** Add a `lyrics` package (parser + LRCLIB client + fetch orchestrator + cache file store) and a `setlist` package (Setlist/SetlistEntry entities + LiveQueue manager) on top of the existing data layer. A new Zen performance view replaces the audio-mode `PlayerScreen` for mp3 songs. The split navigator replaces the single `LibraryScreen` as the primary launch screen; the old library list is retained as the right-pane body. Mp4 video playback is untouched — the existing full-screen video `PlayerScreen` handles those unchanged.

**Tech Stack additions:** OkHttp 4.12 + Retrofit 2.9 + kotlinx-serialization 1.6 (JSON) + DataStore Preferences 1.1. No change to Compose / Hilt / Media3 / Room versions.

**Spec:** `docs/superpowers/specs/2026-10-21-dad-music-app-design.md`

## Global Constraints

- All constraints from M1 remain in force (min SDK 26, 100% Kotlin, SAF-only file access, dark-only theme, strings.xml, 64 dp primary / 48 dp secondary tap targets, no crash reporting SDK yet).
- **Network use is confined to a single Retrofit interface** targeting `https://lrclib.net`. No other hosts. LRCLIB has no auth; include a `User-Agent: DadMusic/1.x (<github url later>)` header per their etiquette guide.
- **Offline-first:** every lyric fetch must be cancellable, must never block UI thread, and failure must leave the song in a usable "no lyrics" state — never a crash and never a half-written cache file.
- **Live queue is strictly device-local.** Its Room table has NO `modifiedUtc`/`deviceId` columns and is excluded from any future sync manifest.
- **No lyric is overlaid on video.** mp4 playback uses the existing full-screen `PlayerScreen`; the new Zen view is strictly for `FileType.AUDIO`.
- **Pre-existing `.lrc` files on disk preempt LRCLIB.** If `<basename>.lrc` sits next to a music file during scan, its content is adopted and the LRCLIB fetch for that song is skipped — never overwritten.
- Internal caches (fetched LRC text) go under `context.filesDir/lyrics/<songId>.lrc`. User's disk `.lrc` files are read-only — we never write back to the user's folder.
- Room schema bumps: v3 adds Lyrics columns to Song; v4 adds Setlist/SetlistEntry/LiveQueueEntry tables. `fallbackToDestructiveMigration()` remains the M2 strategy; proper migrations are M3 scope.

## Review Focus

Six input classes the spec implies but tasks must explicitly cover:

1. **LRCLIB offline / 404 / malformed JSON** — fetch must map every failure to a readable "no lyrics" state and NOT block the UI; pinned to Task 5 and Task 6.
2. **Pre-existing `.lrc` file next to a song** — scan must detect it, parse it, and the song's `lyricsStatus` must become `ADOPTED_FROM_DISK` before any network fetch runs; pinned to Task 3.
3. **Live queue persistence across process death** — if the app is killed mid-set, re-opening the setlist must restore the exact live queue (not the saved template); pinned to Task 9.
4. **Lyric timing drift under Bluetooth output** — a global lyric-offset preference must shift all current-line selection calculations; pinned to Task 12 + Task 13.
5. **Request-add undo race** — tapping Undo on the 10 s toast must remove the just-added entry even if the user has interacted with other UI in between; pinned to Task 11.
6. **Setlist reorder vs. saved template** — reordering in the live queue must NOT mutate the saved Setlist; the "Save changes" button is the only path to persist. Pinned to Task 10.

Each is pinned to a task below.

---

## File Structure

```
app/src/main/java/com/cameronbatten/dadmusic/
├── data/
│   ├── db/
│   │   ├── Song.kt                              # +lyricsStatus, +lyricsCachePath columns
│   │   ├── Lyrics.kt                            # NEW — @Entity (songId FK)
│   │   ├── LyricsDao.kt                         # NEW
│   │   ├── Setlist.kt                           # NEW — @Entity
│   │   ├── SetlistEntry.kt                      # NEW — @Entity (compound PK)
│   │   ├── SetlistDao.kt                        # NEW
│   │   ├── LiveQueueEntry.kt                    # NEW — @Entity (device-local)
│   │   └── LiveQueueDao.kt                      # NEW
│   ├── scan/
│   │   ├── LrcSidecarFinder.kt                  # NEW — looks up <basename>.lrc beside a song
│   │   └── LibraryImporter.kt                   # MODIFIED — adopts sidecar LRCs; creates setlists
│   └── folder/
│       └── FolderRegistry.kt                    # MODIFIED — exposes human-readable folder names
├── lyrics/
│   ├── LrcParser.kt                             # NEW — pure Kotlin fn, LRC text → List<TimedLine>
│   ├── LrcCacheStore.kt                         # NEW — write / read filesDir/lyrics/<songId>.lrc
│   ├── net/
│   │   ├── LrclibApi.kt                         # NEW — Retrofit interface
│   │   ├── LrclibDtos.kt                        # NEW — @Serializable DTOs
│   │   └── NetworkModule.kt                     # NEW — Hilt @Provides for OkHttp + Retrofit
│   ├── LyricsRepository.kt                      # NEW — orchestrates fetch, cache, DB write
│   └── BulkLyricsFetchJob.kt                    # NEW — walks songs with status MISSING
├── playback/
│   └── PositionTicker.kt                        # NEW — Flow<Long> at 60 Hz tied to Player
├── setlist/
│   └── LiveQueueManager.kt                      # NEW — in-memory + DB-persisted queue facade
├── ui/
│   ├── navigator/
│   │   ├── NavigatorScreen.kt                   # NEW — split pane (setlist left, library right)
│   │   └── NavigatorViewModel.kt                # NEW
│   ├── setlist/
│   │   ├── SetlistPane.kt                       # NEW — left pane, dropdown + live queue
│   │   └── LiveQueueRow.kt                      # NEW
│   ├── search/
│   │   ├── SearchPane.kt                        # NEW — right pane, search field + results
│   │   └── SearchViewModel.kt                   # NEW
│   ├── zen/
│   │   ├── ZenPlayerScreen.kt                   # NEW — 3-line auto-scroll
│   │   └── ZenPlayerViewModel.kt                # NEW
│   └── settings/
│       ├── SettingsScreen.kt                    # NEW
│       └── SettingsViewModel.kt                 # NEW
├── preferences/
│   ├── UserPreferences.kt                       # NEW — DataStore wrapper
│   └── PreferencesModule.kt                     # NEW — Hilt binding
└── MainNavHost.kt                               # MODIFIED — routes: navigator / zen / player / folders / settings
```

Existing `LibraryScreen.kt` is retained but demoted — it becomes the body of `SearchPane.kt` (right pane). The mini-player bar from M1 moves to live under the navigator.

---

## Task 1: Lyrics data model + DAO

**Files:**
- Create: `data/db/Lyrics.kt`, `data/db/LyricsDao.kt`
- Modify: `data/db/Song.kt` (add `lyricsStatus: LyricsStatus`, `lyricsCachePath: String?`), `data/db/AppDatabase.kt` (version 3), `di/AppModule.kt` (provide `LyricsDao`)
- Test: `app/src/androidTest/.../data/db/LyricsDaoTest.kt`

**Interfaces:**
- Produces:
  - `enum class LyricsStatus { MISSING, FETCHING, ADOPTED_FROM_DISK, FETCHED_SYNCED, FETCHED_UNSYNCED, FAILED }`
  - `data class Lyrics(id: Long, songId: Long, rawLrc: String, hasTimings: Boolean, source: LyricsSource, modifiedUtc: Long)`
  - `enum class LyricsSource { DISK_SIDECAR, LRCLIB, USER_EDIT }`
  - `LyricsDao` with: `suspend fun upsert(lyrics: Lyrics): Long`, `suspend fun findBySongId(id: Long): Lyrics?`, `fun observeBySongId(id: Long): Flow<Lyrics?>`

- [ ] **Step 1: Write failing DAO tests**
- [ ] **Step 2: Run and verify tests fail**
- [ ] **Step 3: Implement entity + DAO + schema bump; migrate `Song` additions**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(data): add Lyrics entity + status columns on Song`

---

## Task 2: LRC parser (pure function)

**Files:**
- Create: `lyrics/LrcParser.kt`
- Test: `app/src/test/.../lyrics/LrcParserTest.kt` *(JVM unit test — pure function)*

**Interfaces:**
- Produces:
  - `data class TimedLine(val startMs: Long, val text: String)`
  - `object LrcParser { fun parse(lrcText: String): List<TimedLine> }`

- [ ] **Step 1: Write failing parametrized tests**

Cases:
- Standard LRC with `[mm:ss.xx]` tags.
- `[mm:ss]` with no centiseconds.
- Multiple identical timestamps on consecutive lines (preserved in order).
- Blank lines preserved as empty `TimedLine` (half-weight placeholder).
- Unsynced lyrics (no `[` prefix) → returns a single entry with `startMs = -1L` for each line (caller decides linear scroll).
- Malformed timestamp like `[00:xx.yy]` → skip that line, keep others.

- [ ] **Step 2: Run and verify tests fail**
- [ ] **Step 3: Implement `parse`** — regex `\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]`, iterate, collect.
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(lyrics): LRC parser with timestamp + unsynced handling`

---

## Task 3: Pre-existing `.lrc` sidecar adoption (Review Focus #2)

**Files:**
- Create: `data/scan/LrcSidecarFinder.kt`
- Modify: `data/scan/LibraryImporter.kt` — adopt sidecar during the same loop that upserts the Song
- Test: `app/src/androidTest/.../data/scan/LrcSidecarFinderTest.kt`, add cases to `LibraryImporterTest`

**Interfaces:**
- Consumes: `ContentResolver` for SAF reads; `LyricsDao`.
- Produces:
  - `class LrcSidecarFinder @Inject constructor(contentResolver: ContentResolver)` with `suspend fun findFor(song: Song, folderTree: DocumentFile): String?` — returns raw LRC text or null.
  - `LibraryImporter` upserts a `Lyrics(source = DISK_SIDECAR)` row when a sidecar is found; sets `Song.lyricsStatus = ADOPTED_FROM_DISK`.

- [ ] **Step 1: Write failing tests** — including a case where a sidecar exists but LRCLIB fetch would try to run (verify fetch is skipped).
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement finder + importer branch**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(lyrics): honor .lrc sidecar files during library scan`

---

## Task 4: Lyric cache file store

**Files:**
- Create: `lyrics/LrcCacheStore.kt`
- Test: `app/src/androidTest/.../lyrics/LrcCacheStoreTest.kt`

**Interfaces:**
- Produces: `class LrcCacheStore @Inject constructor(@ApplicationContext context: Context)` with:
  - `suspend fun write(songId: Long, lrcText: String): String` — returns the absolute path written (atomic: write to `.tmp`, then rename).
  - `suspend fun read(songId: Long): String?` — null if missing.
  - `suspend fun delete(songId: Long)` — idempotent.

- [ ] **Step 1: Write failing tests** — including an atomic-write test (crash during write leaves no partial file).
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement using `context.filesDir / "lyrics" / "<id>.lrc"`**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(lyrics): atomic LRC cache file store under filesDir`

---

## Task 5: LRCLIB Retrofit client (Review Focus #1)

**Files:**
- Create: `lyrics/net/LrclibApi.kt`, `lyrics/net/LrclibDtos.kt`, `lyrics/net/NetworkModule.kt`
- Modify: `app/build.gradle.kts` (add OkHttp 4.12, Retrofit 2.9, Retrofit kotlinx-serialization converter, kotlinx-serialization 1.6; add the kotlinx-serialization plugin)
- Test: `app/src/test/.../lyrics/net/LrclibApiTest.kt` — uses OkHttp `MockWebServer`.

**Interfaces:**
- Produces:
  - `@Serializable data class LrclibGetResponse(id: Long, trackName: String, artistName: String, syncedLyrics: String?, plainLyrics: String?, duration: Double)`
  - `interface LrclibApi { @GET("/api/get") suspend fun get(@Query("track_name") title: String, @Query("artist_name") artist: String?, @Query("album_name") album: String?, @Query("duration") durationSec: Double?): LrclibGetResponse }`
  - `NetworkModule` with `@Provides @Singleton` for `OkHttpClient` (10 s connect/read timeout, `User-Agent` header), `Retrofit`, `LrclibApi`.

- [ ] **Step 1: Write failing tests** — success, 404 → `HttpException`, malformed JSON → `SerializationException`, timeout → `IOException`.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Add dependencies + apply plugin + implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(lyrics): LRCLIB Retrofit client with error-mapped tests`

---

## Task 6: Bulk lyrics fetch job (Review Focus #1)

**Files:**
- Create: `lyrics/LyricsRepository.kt`, `lyrics/BulkLyricsFetchJob.kt`
- Test: `app/src/androidTest/.../lyrics/BulkLyricsFetchJobTest.kt`

**Interfaces:**
- Consumes: `LrclibApi`, `LrcCacheStore`, `LyricsDao`, `SongDao`.
- Produces:
  - `class LyricsRepository` with `suspend fun fetchFor(song: Song): LyricsFetchResult` — returns one of `Fetched(hasTimings: Boolean)`, `NotFound`, `OfflineOrFailed(reason: String)`. Always transitions `Song.lyricsStatus`.
  - `data class FetchProgress(val completed: Int, val total: Int, val failed: Int)`
  - `class BulkLyricsFetchJob` with `fun run(): Flow<FetchProgress>` — walks every `Song` with `lyricsStatus == MISSING`, fetches, writes cache + DB, emits progress. Cancellable via Flow collection cancellation.

- [ ] **Step 1: Write failing tests** — all-success, half-fail, all-offline, cancel-mid-run.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(lyrics): bulk LRCLIB fetch with progress + cancellation`

---

## Task 7: Setlist + SetlistEntry data model

**Files:**
- Create: `data/db/Setlist.kt`, `data/db/SetlistEntry.kt`, `data/db/SetlistDao.kt`
- Modify: `data/db/AppDatabase.kt` (version 4), `di/AppModule.kt`
- Test: `app/src/androidTest/.../data/db/SetlistDaoTest.kt`

**Interfaces:**
- Produces:
  - `data class Setlist(id: Long, name: String, sourceFolderUri: String?, orderIndex: Int, createdAt: Long)`
  - `data class SetlistEntry(setlistId: Long, songId: Long, position: Int)` with compound PK `(setlistId, songId)`.
  - `SetlistDao` with `suspend fun insertSetlist(s: Setlist): Long`, `fun observeAll(): Flow<List<Setlist>>`, `suspend fun entriesFor(setlistId: Long): List<SongWithPosition>`, `fun observeEntries(setlistId: Long): Flow<List<SongWithPosition>>`, `suspend fun replaceEntries(setlistId: Long, entries: List<SetlistEntry>)` (transaction).
  - `data class SongWithPosition(song: Song, position: Int)`

- Position uses **sparse integers** (1000, 2000, 3000…) per spec §7.2.

- [ ] **Step 1: Write failing DAO tests** — insert, observe, replaceEntries transactionally.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement entities + DAO + schema bump**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(data): Setlist + SetlistEntry with sparse positions`

---

## Task 8: Folder-to-setlist bridge

**Files:**
- Modify: `data/scan/LibraryImporter.kt` — on first import from a folder, create a Setlist with the folder's display name and populate SetlistEntry rows for every song found under that folder. On re-scan, update the entry set, preserving existing positions where the song URI survives.
- Test: `app/src/androidTest/.../data/scan/LibraryImporterTest.kt` — add setlist assertions.

**Interfaces:** No new types; the orchestrator just gains a "setlist upsert" phase after the song-upsert phase.

- [ ] **Step 1: Write failing tests** — one folder → one setlist, rename folder (via FolderRegistry) → setlist name updates, remove folder → setlist is deleted + its entries pruned.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(data): every folder becomes a setlist on scan`

---

## Task 9: LiveQueue entity + manager (Review Focus #3)

**Files:**
- Create: `data/db/LiveQueueEntry.kt`, `data/db/LiveQueueDao.kt`, `setlist/LiveQueueManager.kt`
- Modify: `data/db/AppDatabase.kt` (same v4 bump as Task 7 covers this table too)
- Test: `app/src/androidTest/.../setlist/LiveQueueManagerTest.kt`

**Interfaces:**
- Produces:
  - `data class LiveQueueEntry(position: Int, songId: Long, source: QueueSource, parentSetlistId: Long?)` — PK is `position`.
  - `enum class QueueSource { SAVED, REQUEST }`
  - `class LiveQueueManager @Inject constructor(dao: LiveQueueDao)` with:
    - `fun observe(): Flow<List<LiveQueueEntry>>`
    - `suspend fun openSetlist(setlistId: Long)` — wipes queue; writes queue copy from the saved setlist's entries.
    - `suspend fun append(songId: Long, source: QueueSource): LiveQueueEntry`
    - `suspend fun remove(position: Int)`
    - `suspend fun move(fromPosition: Int, toPosition: Int)` — sparse-int renumbering if gap collapses.
    - `fun currentParentSetlistId(): Long?`

Live-queue table has NO `modifiedUtc`/`deviceId`. Explicitly flagged "device-local" in a KDoc comment on the entity.

- [ ] **Step 1: Write failing tests** — covers Review Focus #3 by simulating process death: construct a `LiveQueueManager`, append entries, destroy, create a fresh manager instance on the same DB, assert the queue state round-trips.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(setlist): persistent device-local live queue`

---

## Task 10: Split navigator UI (Review Focus #6)

**Files:**
- Create: `ui/navigator/NavigatorScreen.kt`, `ui/navigator/NavigatorViewModel.kt`, `ui/setlist/SetlistPane.kt`, `ui/setlist/LiveQueueRow.kt`
- Modify: `MainNavHost.kt` — `startDestination = "navigator"`.
- Test: `app/src/androidTest/.../ui/navigator/NavigatorScreenTest.kt`

**Interfaces:**
- `NavigatorViewModel` exposes `activeSetlistId: StateFlow<Long?>`, `allSetlists: StateFlow<List<Setlist>>`, `liveQueue: StateFlow<List<LiveQueueEntry>>`, `fun switchTo(setlistId: Long?)` (null = library virtual view), `fun reorder(from: Int, to: Int)`, `fun removeFromQueue(position: Int)`, `fun saveQueueToParent()` (only enabled when live queue differs from parent setlist).

- Landscape (tablet default) shows two panes side by side (`Row` with `.weight`). Portrait collapses to one pane with a toggle in the top app bar. Use `BoxWithConstraints` on `maxWidth > 600 dp` to pick layout.
- "NOW PLAYING" marker on the row whose `songId == playbackState.currentSongId`. Dim + strikethrough rows whose position < current.

- [ ] **Step 1: Write failing tests** — rendering, reorder gesture updates ViewModel, "Save changes" button appears only when queue diverges from parent, dropdown switch wipes + reloads queue.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(ui): split navigator with setlist dropdown and live queue reorder`

---

## Task 11: Search pane with one-tap add + 10 s undo (Review Focus #5)

**Files:**
- Create: `ui/search/SearchPane.kt`, `ui/search/SearchViewModel.kt`
- Modify: `ui/library/LibraryScreen.kt` — demoted to a private composable rendered by `SearchPane`; keep its mini-player.
- Test: `app/src/androidTest/.../ui/search/SearchPaneTest.kt`

**Interfaces:**
- `SearchViewModel` exposes `query: StateFlow<String>`, `results: StateFlow<List<Song>>` (filtered by title + artist, substring, case-insensitive), `fun onAddToQueue(song: Song)` → appends to live queue + schedules a 10 s undo window via a `Channel<UndoEvent>`.
- `UndoEvent(songId: Long, addedAtMs: Long, livePosition: Int)` with `fun undo()` removing the entry if still within window.
- Snackbar surfaced by `NavigatorScreen` consuming the channel.

- [ ] **Step 1: Write failing tests** — undo removes the entry; undo after 10 s is a no-op; undo after user has added two more songs still removes the correct one (not the latest).
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(ui): request-add from search with 10 s undo`

---

## Task 12: Zen performance view (Review Focus #4)

**Files:**
- Create: `ui/zen/ZenPlayerScreen.kt`, `ui/zen/ZenPlayerViewModel.kt`, `playback/PositionTicker.kt`
- Modify: `MainNavHost.kt` — route `zen/{songId}`; audio playback now navigates here, video still goes to `player/{songId}` (the M1 full-screen video view).
- Test: `app/src/androidTest/.../ui/zen/ZenPlayerScreenTest.kt`

**Interfaces:**
- `PositionTicker` emits `Flow<Long>` of `player.currentPosition` at ~60 Hz while playing; pauses when not playing. Wired to `PlaybackSession.getPlayer()`.
- `ZenPlayerViewModel` computes `currentLineIndex: StateFlow<Int>` by binary-searching `List<TimedLine>` with `(position + offsetMs)`. Exposes previous / current / next line text + `isPlaying` + `durationMs`.
- `ZenPlayerScreen` renders a vertically-centred 3-line stack: previous (dimmed, 32 sp), current (highlighted primary accent, 64 sp, bold, scaled), next (dimmed, 32 sp). Single tap anywhere summons the transport overlay (play/pause/skip/scrubber) for 3 s. Screen stays on via `KeepScreenOn()`.
- Unsynced lyrics (`startMs == -1L` throughout): distribute evenly across `durationMs`, treating blank lines as half-weight gaps.

- [ ] **Step 1: Write failing tests** — line advance at correct ms boundary, offset shifts the boundary, tap reveals controls then auto-fades, blank lines handled, unsynced fallback distributes evenly.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(ui): Zen performance view with 3-line auto-scroll`

---

## Task 13: Lyric offset + font-size preferences (Review Focus #4)

**Files:**
- Create: `preferences/UserPreferences.kt`, `preferences/PreferencesModule.kt`
- Modify: `app/build.gradle.kts` (DataStore Preferences dep); `ZenPlayerViewModel` to collect offset.
- Test: `app/src/test/.../preferences/UserPreferencesTest.kt` (JVM, use `PreferenceDataStoreFactory.create` with a temp file)

**Interfaces:**
- `UserPreferences` exposes `val lyricOffsetMs: Flow<Long>`, `suspend fun setLyricOffsetMs(ms: Long)`, `val fontSizePreset: Flow<FontSizePreset>`, `suspend fun setFontSizePreset(FontSizePreset)`.
- `enum class FontSizePreset(val currentLineSp: Int) { SMALL(48), MEDIUM(64), LARGE(80) }`

- [ ] **Step 1: Write failing tests** — round-trip, default values, concurrent reads.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(preferences): DataStore-backed lyric offset + font size`

---

## Task 14: Settings screen

**Files:**
- Create: `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`
- Modify: `MainNavHost.kt` — route `settings`; `NavigatorScreen` gains a gear-icon action.
- Test: `app/src/androidTest/.../ui/settings/SettingsScreenTest.kt`

**Interfaces:**
- `SettingsViewModel` exposes `uiState: StateFlow<SettingsUiState>` and `fun setFontSize(FontSizePreset)`, `fun setLyricOffset(ms: Long)`, `fun refetchAllLyrics()` (kicks `BulkLyricsFetchJob`), `fun clearLyricsCache()`.
- Screen: three sections — Lyrics (offset slider -2000..+2000 ms, font-size preset chips), Library ("Re-fetch all lyrics" button with progress text), About (version, link to spec).

- [ ] **Step 1: Write failing tests** — slider updates preference, re-fetch button shows progress.
- [ ] **Step 2: Run tests — fail**
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests — pass**
- [ ] **Step 5: Commit** — `feat(ui): settings screen with lyric offset and bulk re-fetch`

---

## Task 15: Navigation wiring + manual smoke + release build

**Files:**
- Modify: `MainNavHost.kt`, `README.md`
- Create: `docs/superpowers/manual-smoke-M2.md`

- [ ] **Step 1: Wire routes** — `navigator` (start) → `zen/{songId}` for audio, `player/{songId}` for video, `folders`, `settings`. Mini-player on `navigator` deep-links to the right destination.
- [ ] **Step 2: Build release APK** — `./gradlew :app:assembleRelease`.
- [ ] **Step 3: Update README** — call out network permission + LRCLIB attribution block.
- [ ] **Step 4: Manual smoke on emulator + physical Xiaomi** — walk every item in `manual-smoke-M2.md`.
- [ ] **Step 5: Commit** — `feat: M2 navigation + manual smoke + release build`

---

## Acceptance Criteria

M2 is done when, on a signed release APK installed on the Xiaomi tablet:

1. A folder with a mix of mp3s (some with `.lrc` sidecars, some without) imports into one Setlist whose name matches the folder, and every song shows a filled-lyric indicator (either from the sidecar or from LRCLIB).
2. Opening the setlist from the navigator loads the live queue, matches the stored order, and the user can drag to reorder without touching the saved template.
3. Searching for a song and tapping it adds it to the end of the queue with a visible 10-second undo toast; tapping Undo removes it; waiting 10 s makes the toast disappear and the song remains.
4. Tapping a song in the queue opens the Zen view: three lines of lyrics advance in sync with playback; a global offset of +200 ms audibly shifts the whole scroll by 200 ms (verify by ear with the aux cable).
5. Rotating the tablet mid-song does not restart playback and does not visually jump the lyric stack.
6. Killing the app mid-set and reopening it restores the live queue exactly, with the current song still cued.
7. Enabling airplane mode and re-importing a folder: existing sidecar `.lrc` files still populate; LRCLIB-only songs land in `FAILED` state without crashing.
8. Settings → "Re-fetch all lyrics" with network back on cleanly populates every `FAILED` or `MISSING` song.

---

*End of Milestone 2 plan.*
