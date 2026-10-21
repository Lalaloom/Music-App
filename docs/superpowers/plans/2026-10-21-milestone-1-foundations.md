# Milestone 1: Foundations + Local Library + Basic Playback

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a signed APK that lets a user pick a music folder on their Android device, see their mp3 and mp4 files as a library with recognized titles and artists, and play any song with basic controls.

**Architecture:** Native Android app in Kotlin + Jetpack Compose. Media3 ExoPlayer runs playback inside a MediaSessionService so controls survive backgrounding. Room (SQLite) stores the song library locally. Scoped-Storage-compliant SAF folder picker is the only way files enter the library. No lyrics, no setlists, no sync, no cloud, no YouTube in this milestone — those arrive in M2–M5.

**Tech Stack:** Kotlin 1.9+, Jetpack Compose (Material 3), AndroidX Media3 (ExoPlayer + MediaSession), Room, Hilt, Kotlin Coroutines + Flow, JUnit 4 + Compose UI test, Gradle Kotlin DSL.

**Spec:** `docs/superpowers/specs/2026-10-21-dad-music-app-design.md`

## Global Constraints

- Minimum SDK: **26 (Android 8.0)**. Target SDK: **latest stable** at build time.
- Package: `com.cameronbatten.dadmusic`.
- 100% Kotlin. No Java sources.
- All file access via Android **Storage Access Framework (SAF)**. No `READ_EXTERNAL_STORAGE` permission.
- Dark theme is the only theme in M1. No light theme toggle.
- All user-facing strings in `strings.xml` — no hardcoded English in Composables (prep for i18n without doing it).
- All tap-target primary actions ≥ **64dp**; secondary ≥ **48dp**.
- No network calls in M1. Any network dependency = scope creep; push to M2.
- No crash reporting SDK in M1 (Sentry arrives M5).

## Review Focus

Five input classes the spec implies but tasks must explicitly cover:

1. **SAF URI permission after device reboot** — picked folders must survive reboot via persisted URI permissions; without this, every reboot wipes the library.
2. **Audio focus loss (phone call, other media app, Bluetooth disconnect)** — ExoPlayer must pause and respect audio focus; critical mid-gig.
3. **Files with missing or empty ID3 / mp4 metadata** — common in the author's library; must fall back cleanly to filename parsing, not crash.
4. **File removed from disk between scan and playback** — SAF URI resolves to a now-missing file; playback must surface a readable error, not crash.
5. **Configuration change (rotation) during playback** — Compose + MediaSessionService state must persist; audio must not restart or stop.

Each is pinned to a task below.

---

## File Structure

```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/cameronbatten/dadmusic/
│   │   ├── DadMusicApp.kt                   # @HiltAndroidApp
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── Song.kt                  # @Entity
│   │   │   │   └── SongDao.kt
│   │   │   ├── scan/
│   │   │   │   ├── FileScanner.kt           # SAF traversal
│   │   │   │   ├── FilenameParser.kt        # pure fn
│   │   │   │   ├── MetadataReader.kt        # Media3 extractor
│   │   │   │   └── LibraryImporter.kt       # orchestrator
│   │   │   └── folder/
│   │   │       └── FolderRegistry.kt        # persisted URIs
│   │   ├── playback/
│   │   │   ├── PlaybackService.kt           # MediaSessionService
│   │   │   ├── PlaybackController.kt        # app-side facade
│   │   │   └── PlaybackState.kt             # data class
│   │   ├── ui/
│   │   │   ├── theme/                       # dark Material 3
│   │   │   ├── library/
│   │   │   │   ├── LibraryScreen.kt
│   │   │   │   └── LibraryViewModel.kt
│   │   │   ├── player/
│   │   │   │   ├── PlayerScreen.kt
│   │   │   │   └── PlayerViewModel.kt
│   │   │   └── folders/
│   │   │       └── FolderPickerScreen.kt
│   │   └── di/
│   │       └── AppModule.kt
│   └── res/values/strings.xml
└── src/test/.../*                           # JVM unit tests
└── src/androidTest/.../*                    # instrumented tests
```

---

## Task 1: Project scaffolding

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts` (root), `app/build.gradle.kts`, `gradle.properties`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/cameronbatten/dadmusic/DadMusicApp.kt`, `app/src/main/java/com/cameronbatten/dadmusic/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `.gitignore`, `README.md`
- Test: `app/src/test/java/com/cameronbatten/dadmusic/SmokeTest.kt`

**Interfaces:**
- Produces: `DadMusicApp` (Hilt application class), `MainActivity` empty Compose host, dark Material 3 theme, Gradle build that produces a signed debug APK.

- [ ] **Step 1: Write smoke test**

```kotlin
class SmokeTest {
    @Test fun placeholder() { assert(2 + 2 == 4) }
}
```

- [ ] **Step 2: Run and verify it fails (project doesn't build yet)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: FAIL (Gradle build error — no project).

- [ ] **Step 3: Initialize Android project**

Create Gradle wrapper via `gradle wrapper --gradle-version 8.5`. Configure `app/build.gradle.kts` with:
- `android.compileSdk = 34`, `minSdk = 26`, `targetSdk = 34`
- `kotlinOptions.jvmTarget = "17"`
- Compose BOM 2024.02 or later; Material 3; Hilt 2.50; Room 2.6; Media3 1.2+
- `applicationId = "com.cameronbatten.dadmusic"`
- Debug + release build types; debug signed with debug keystore

Create `DadMusicApp` with `@HiltAndroidApp`. Create `MainActivity` with `@AndroidEntryPoint` and an empty `setContent {}`. Register both in manifest. Add a `DadMusicTheme` dark-only Material 3 theme in `ui/theme/`.

- [ ] **Step 4: Run smoke test — passes**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Build and install APK on connected device**

Run: `./gradlew :app:installDebug`
Expected: APK installs; launching shows a blank dark screen without crashing.

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "chore: scaffold Android project with Compose, Hilt, Media3, Room"
```

---

## Task 2: Song entity, DAO, and Room database

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/data/db/Song.kt`, `SongDao.kt`, `AppDatabase.kt`, `app/src/main/java/com/cameronbatten/dadmusic/di/AppModule.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/data/db/SongDaoTest.kt`

**Interfaces:**
- Consumes: Hilt application context from Task 1.
- Produces:
    - `Song(id: Long, title: String, artist: String?, album: String?, uri: String, fileType: FileType, durationMs: Long, dateAdded: Long, confirmed: Boolean)`
    - `enum class FileType { AUDIO, VIDEO }`
    - `SongDao` with `suspend fun insert(song: Song): Long`, `suspend fun insertAll(songs: List<Song>): List<Long>`, `fun observeAll(): Flow<List<Song>>`, `suspend fun deleteByUri(uri: String)`, `suspend fun findByUri(uri: String): Song?`
    - `AppDatabase: RoomDatabase` with `fun songDao(): SongDao`
    - `AppModule` provides `AppDatabase` and `SongDao` as `@Singleton`

- [ ] **Step 1: Write failing DAO tests**

Test names:
- `insert_assignsAutoId`
- `insertAll_persistsEveryRow`
- `observeAll_emitsSortedByTitle`
- `findByUri_returnsNullWhenMissing`
- `deleteByUri_removesRowAndFutureObservationsExcludeIt`

Each test uses `Room.inMemoryDatabaseBuilder` + `allowMainThreadQueries()`.

- [ ] **Step 2: Run and verify tests fail (classes don't exist yet)**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: FAIL to compile.

- [ ] **Step 3: Implement entity, DAO, database, and Hilt module**

Create `Song` entity with the signature above (annotate `@Entity(tableName = "songs")`, `@PrimaryKey(autoGenerate = true)`, `@ColumnInfo` as needed). `uri` is uniquely indexed. `fileType` is persisted as String via a `@TypeConverter`. Create `SongDao` with the signatures above. Create `AppDatabase` as `RoomDatabase` with `version = 1` and no exported schema in M1. Create `AppModule` with `@Provides` for both.

- [ ] **Step 4: Run tests — pass**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): add Song entity, SongDao, Room database"
```

---

## Task 3: Folder registry (persisted SAF URIs)

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/data/folder/FolderRegistry.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/data/folder/FolderRegistryTest.kt`

**Interfaces:**
- Consumes: `ContentResolver` from Hilt.
- Produces: `class FolderRegistry @Inject constructor(context: Context)` with:
    - `suspend fun add(uri: Uri)` — calls `takePersistableUriPermission(READ)`, stores the string in a SharedPreferences-backed set.
    - `suspend fun remove(uri: Uri)` — releases permission and removes from set.
    - `fun observe(): Flow<Set<Uri>>` — Flow of persisted URIs.
    - `fun resolvableUris(): List<Uri>` — intersects stored URIs with `contentResolver.persistedUriPermissions`, dropping any that no longer resolve.

- [ ] **Step 1: Write failing tests covering Review Focus item 1**

Tests:
- `addGrantsPersistableReadPermissionOnTheUri` — verify `persistedUriPermissions` contains the added URI after `add`.
- `resolvableUris_dropsUrisWithoutActivePermission` — add a URI, release its permission manually, assert it's filtered out.
- `observe_emitsAfterAddAndAfterRemove` — collect first two emissions and assert set membership.

The second test is the one that pins Review Focus #1 (SAF permission survives / doesn't after reboot — we simulate by releasing permission directly).

- [ ] **Step 2: Run and verify tests fail**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*FolderRegistryTest*"`
Expected: FAIL (class missing).

- [ ] **Step 3: Implement `FolderRegistry`**

Store URIs as `Set<String>` in a dedicated `SharedPreferences` file (`"folder_registry"`). Call `contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)` on add and `releasePersistableUriPermission` on remove. `resolvableUris()` reads SharedPreferences + `contentResolver.persistedUriPermissions` and returns the intersection.

- [ ] **Step 4: Run tests — pass**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*FolderRegistryTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): persist SAF folder URIs with permission tracking"
```

---

## Task 4: File scanner (SAF folder traversal)

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/data/scan/FileScanner.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/data/scan/FileScannerTest.kt`

**Interfaces:**
- Consumes: `FolderRegistry` from Task 3; `ContentResolver`.
- Produces:
    - `data class DiscoveredFile(val uri: Uri, val displayName: String, val extension: String, val mimeType: String?, val sizeBytes: Long)`
    - `class FileScanner @Inject constructor(context: Context, folderRegistry: FolderRegistry)` with `suspend fun scanAll(): List<DiscoveredFile>` — walks every resolvable folder recursively via `DocumentFile.fromTreeUri(...).listFiles()`, filters by extensions `mp3, m4a, wav, flac, aac, mp4, mkv, webm`.

- [ ] **Step 1: Write failing tests**

Tests (use a scratch external storage dir and `MediaStore`-less fixtures):
- `scanAll_returnsFilesFromAllRegisteredFolders`
- `scanAll_skipsNonMusicExtensions`
- `scanAll_includesFilesInSubdirectories`
- `scanAll_dedupesIfSameUriAcrossRegisteredFolders`

- [ ] **Step 2: Run tests — fail**

Expected: FAIL (class missing).

- [ ] **Step 3: Implement `FileScanner`**

Use `DocumentFile.fromTreeUri` on each resolvable URI, recursively walk with a `Deque<DocumentFile>`. Filter by extension (case-insensitive). Dedupe by URI. Return list.

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): recursive SAF file scanner with extension filter"
```

---

## Task 5: Filename parser (pure function)

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/data/scan/FilenameParser.kt`
- Test: `app/src/test/java/com/cameronbatten/dadmusic/data/scan/FilenameParserTest.kt` *(JVM unit test — pure function, no Android)*

**Interfaces:**
- Produces:
    - `data class ParsedFilename(val title: String?, val artist: String?)`
    - `object FilenameParser { fun parse(displayName: String): ParsedFilename }`

- [ ] **Step 1: Write failing parametrized tests**

Cases (one `@Test` method each or JUnit `@ParameterizedTest`):
- `"Hotel California - Eagles.mp3"` → `title="Hotel California", artist="Eagles"`
- `"Eagles - Hotel California.mp3"` → `title="Hotel California", artist="Eagles"` *(see note)*
- `"Track 13.mp3"` → `title=null, artist=null`
- `"NoExtension"` → `title=null, artist=null`
- `"Single Word.mp3"` → `title="Single Word", artist=null`
- `"  Padded  -  Spaces  .mp3"` → trimmed values.
- `""` → `title=null, artist=null`

Note on direction: the spec's example uses `"Title - Artist"`. Default to that ordering. If the user later finds many files are "Artist - Title," add a config toggle in M3. The test enforces one direction here.

- [ ] **Step 2: Run and verify tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*FilenameParserTest*"`
Expected: FAIL (object missing).

- [ ] **Step 3: Implement `parse`**

Strip extension, split on `" - "` (space-hyphen-space, exactly). If zero splits: `title = trimmed name, artist = null`. If one split: `title = left, artist = right`. If more: title = whole-before-last-split, artist = last split. Any empty-trimmed result → null for that field.

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): filename title-artist parser with edge cases"
```

---

## Task 6: Media metadata reader (ID3 + mp4)

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/data/scan/MetadataReader.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/data/scan/MetadataReaderTest.kt` *(instrumented — needs real decoder)*
- Test fixtures: `app/src/androidTest/assets/` — tiny sample mp3 with tags, tagless mp3, sample mp4 (karaoke-style silent clip), corrupt file.

**Interfaces:**
- Produces:
    - `data class FileMetadata(val title: String?, val artist: String?, val album: String?, val durationMs: Long)`
    - `class MetadataReader @Inject constructor(context: Context)` with `suspend fun read(uri: Uri): FileMetadata` — reads via `MediaMetadataRetriever`. Returns all-null and `0L` on failure. **Must not throw** for Review Focus #3.

- [ ] **Step 1: Write failing tests, including Review Focus #3**

Tests:
- `read_mp3WithId3_returnsTitleArtistAlbumDuration`
- `read_mp3WithoutTags_returnsNullsAndNonZeroDuration` *(Review Focus #3)*
- `read_mp4Karaoke_returnsDuration`
- `read_corruptFile_returnsBlankMetadataNotCrash` *(Review Focus #3)*

- [ ] **Step 2: Run tests — fail**

- [ ] **Step 3: Implement `MetadataReader`**

Open `MediaMetadataRetriever`, call `setDataSource(context, uri)` in a `runCatching`. Pull `METADATA_KEY_TITLE`, `METADATA_KEY_ARTIST`, `METADATA_KEY_ALBUM`, `METADATA_KEY_DURATION`. Always release the retriever in `finally`. On any `Throwable`, return `FileMetadata(null, null, null, 0L)`.

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): media metadata reader with crash-resilient fallback"
```

---

## Task 7: Library importer (orchestrator)

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/data/scan/LibraryImporter.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/data/scan/LibraryImporterTest.kt`

**Interfaces:**
- Consumes: `FileScanner`, `FilenameParser`, `MetadataReader`, `SongDao`.
- Produces:
    - `data class ImportResult(val added: Int, val updated: Int, val skipped: Int)`
    - `class LibraryImporter @Inject constructor(scanner, parser, reader, dao)` with `suspend fun importAll(): ImportResult` — scans, for each file computes `(title, artist, album, durationMs)` by preferring metadata when non-null and falling back to filename parse, infers `fileType` from extension, upserts into DB (by `uri`). `confirmed = (metadata.title != null && metadata.artist != null) || (parsedBoth != null)`.

- [ ] **Step 1: Write failing tests**

Tests:
- `importAll_newFile_insertsAsAdded`
- `importAll_sameUriTwice_countsAsUpdatedNotDuplicated`
- `importAll_metadataPreferredWhenPresent`
- `importAll_fallsBackToFilenameWhenMetadataMissing`
- `importAll_mp4File_fileTypeIsVIDEO`

- [ ] **Step 2: Run tests — fail**

- [ ] **Step 3: Implement `importAll`**

For each `DiscoveredFile`: call `MetadataReader.read`, call `FilenameParser.parse`. Merge: title = metadata.title ?: parsed.title ?: displayName. artist = metadata.artist ?: parsed.artist. album = metadata.album. fileType = AUDIO/VIDEO from extension. If row with same URI exists, update; else insert. Count added/updated.

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): library importer merges metadata and filename parse"
```

---

## Task 8: Playback service (Media3 MediaSessionService)

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/playback/PlaybackService.kt`, `PlaybackState.kt`, `PlaybackController.kt`
- Modify: `app/src/main/AndroidManifest.xml` (declare service + `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions, min SDK 34)
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/playback/PlaybackControllerTest.kt`

**Interfaces:**
- Produces:
    - `data class PlaybackState(val currentSongId: Long?, val isPlaying: Boolean, val positionMs: Long, val durationMs: Long)`
    - `class PlaybackController @Inject constructor(context: Context)` — bound to `PlaybackService`'s MediaSession via `MediaController.Builder(...).buildAsync()`. Exposes `fun state(): StateFlow<PlaybackState>`, `suspend fun play(song: Song)`, `fun pause()`, `fun resume()`, `fun seekTo(ms: Long)`, `fun skipNext()`, `fun skipPrevious()`.
    - `class PlaybackService : MediaSessionService` — hosts an `ExoPlayer` with `AudioAttributes` set to `CONTENT_TYPE_MUSIC`, enables audio-focus handling via `setHandleAudioBecomingNoisy(true)` and the default audio-focus handler.

- [ ] **Step 1: Write failing tests covering Review Focus #2, #4, #5**

Tests:
- `play_mp3_statePublishesIsPlayingTrueAndDuration`
- `play_mp4_statePublishesIsPlayingTrue`
- `missingFileUri_errorsCleanlyWithoutCrashing` *(Review Focus #4)*
- `audioBecomesNoisy_pausesPlayback` *(Review Focus #2 — simulate `ACTION_AUDIO_BECOMING_NOISY` broadcast)*
- `processDeathAndRebind_recoversPlaybackState` *(Review Focus #5 — kill and rebind the controller; state should survive via the session)*

- [ ] **Step 2: Run tests — fail**

- [ ] **Step 3: Implement service and controller**

`PlaybackService.onCreate`: build `ExoPlayer` with `AudioAttributes.Builder().setContentType(CONTENT_TYPE_MUSIC).setUsage(USAGE_MEDIA).build()`, call `setHandleAudioBecomingNoisy(true)` on the player, call `setAudioAttributes(..., handleAudioFocus = true)`. Build `MediaSession` with the player. `onGetSession` returns it. `onDestroy` releases player and session.

`PlaybackController`: on init, launch `MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()`. Collect controller state into a `MutableStateFlow<PlaybackState>` via a `Player.Listener`. `play(song)` builds a `MediaItem` from `song.uri` and `setMediaItem`+`prepare`+`play`.

Catch `PlaybackException` on invalid URI, map to a `PlaybackState` with `isPlaying = false` and surface via an event flow consumed by UI (toast later; here just: don't crash).

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(playback): Media3 MediaSessionService with audio focus"
```

---

## Task 9: Folder picker UI

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/ui/folders/FolderPickerScreen.kt`, `FoldersViewModel.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/ui/folders/FolderPickerScreenTest.kt`

**Interfaces:**
- Consumes: `FolderRegistry` (via `FoldersViewModel`), `LibraryImporter`.
- Produces:
    - `class FoldersViewModel @Inject constructor(folderRegistry, libraryImporter)` with `fun folders(): StateFlow<List<Uri>>`, `fun addFolder(uri: Uri)`, `fun removeFolder(uri: Uri)`, `fun rescan()` which runs `LibraryImporter.importAll()` and exposes `importResult: StateFlow<ImportResult?>`.
    - `@Composable fun FolderPickerScreen(onDone: () -> Unit)` — list of folders, "Add folder" button (launches `OpenDocumentTree`), remove button per row, "Scan now" button, progress indicator while importing. **Primary action buttons ≥ 64dp.**

- [ ] **Step 1: Write failing Compose UI test**

Test names:
- `shows_empty_state_when_no_folders`
- `tapping_add_folder_launches_open_document_tree_result` *(use `ActivityScenario` with a stubbed result)*
- `tapping_rescan_disables_button_until_import_finishes`
- `removing_folder_updates_list`

- [ ] **Step 2: Run tests — fail**

- [ ] **Step 3: Implement screen and ViewModel**

ViewModel: `folderRegistry.observe()` → `stateIn` for list; `rescan()` sets `importResult` to null, calls `importer.importAll()` in a `viewModelScope.launch`, publishes the result. Screen: Compose `Scaffold` with top app bar "Your music folders," a `LazyColumn` of folder rows (each `ListItem` with a Trash icon button), an "Add folder" `Button`, a "Scan now" `Button`, and a `LinearProgressIndicator` shown while scan is in progress. Register an `ActivityResultLauncher<Uri?>` for `OpenDocumentTree`; on result, call `viewModel.addFolder(uri)`.

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(ui): folder picker screen with rescan"
```

---

## Task 10: Library list UI

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/ui/library/LibraryScreen.kt`, `LibraryViewModel.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/ui/library/LibraryScreenTest.kt`

**Interfaces:**
- Consumes: `SongDao`, `PlaybackController`.
- Produces:
    - `class LibraryViewModel @Inject constructor(songDao, playbackController)` with `fun songs(): StateFlow<List<Song>>` (sorted by title), `fun onSongTapped(song: Song)` (calls `playbackController.play(song)`), `fun onOpenFolders()` navigation event.
    - `@Composable fun LibraryScreen(onOpenFolders: () -> Unit, onSongPlaying: () -> Unit)` — `LazyColumn` with 72dp rows showing title + artist, dark background, top app bar with a folder-icon action, FAB for folders. Tap row → `onSongPlaying` navigates to Player.

- [ ] **Step 1: Write failing tests**

Tests:
- `shows_songs_from_dao`
- `empty_state_prompts_user_to_add_folders`
- `tap_row_invokes_playbackController_play_and_navigates`

- [ ] **Step 2: Run tests — fail**

- [ ] **Step 3: Implement**

ViewModel: `songDao.observeAll().stateIn(...)`. Screen: `LazyColumn` with row Composable (`Column` of `title` + `artist` subtext, 72dp minHeight, 16dp horizontal padding). Empty state: centered Text + Button "Pick a folder." TopAppBar with an IconButton to open folders.

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(ui): library list screen with tap-to-play"
```

---

## Task 11: Player screen

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/ui/player/PlayerScreen.kt`, `PlayerViewModel.kt`
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/ui/player/PlayerScreenTest.kt`

**Interfaces:**
- Consumes: `PlaybackController`.
- Produces:
    - `class PlayerViewModel @Inject constructor(playbackController)` with `fun state(): StateFlow<PlaybackState>`, `fun playPause()`, `fun next()`, `fun previous()`, `fun seek(ms: Long)`.
    - `@Composable fun PlayerScreen(onBack: () -> Unit)` — dark full-screen view. **For `AUDIO` type:** center text (title, artist) + 64dp primary play/pause button + 48dp next/previous + scrubber. **For `VIDEO` type:** `PlayerView` from Media3 UI, full-screen, controls auto-fade.

- [ ] **Step 1: Write failing tests**

Tests:
- `audio_mode_shows_titleArtist_and_largePlayPause`
- `video_mode_renders_playerview`
- `play_pause_button_toggles_state`
- `rotation_during_playback_keeps_isPlaying_true` *(Review Focus #5 — recreates the activity, confirms state)*

- [ ] **Step 2: Run tests — fail**

- [ ] **Step 3: Implement screen and ViewModel**

ViewModel exposes `state` and delegates to controller. Screen uses `collectAsStateWithLifecycle` on `state`. Branch on `fileType` of the currently-playing song (fetch via a secondary `getCurrent(): Flow<Song?>` on `PlaybackController` — add this method on the controller as part of this task, update tests from Task 8). Audio: `Column` with centered Text and a Row of control IconButtons; a `Slider` tied to `positionMs`/`durationMs`. Video: embed Media3 `PlayerView` via `AndroidView` and attach the controller's player.

- [ ] **Step 4: Run tests — pass**

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(ui): player screen with audio and video modes"
```

---

## Task 12: Navigation wiring + release signing + install docs

**Files:**
- Create: `app/src/main/java/com/cameronbatten/dadmusic/MainNavHost.kt`
- Modify: `MainActivity.kt`, `app/build.gradle.kts` (release signing config placeholder reading from `~/.gradle/gradle.properties`), `README.md`
- No test file (navigation wiring is a trivial coordination layer).

**Interfaces:**
- Consumes: `LibraryScreen`, `PlayerScreen`, `FolderPickerScreen` from Tasks 9–11.
- Produces: a Compose NavHost with routes `library`, `player`, `folders` wired via Navigation Compose, and a buildable release APK via `./gradlew assembleRelease`.

- [ ] **Step 1: Wire NavHost in MainActivity**

Create `MainNavHost` with `NavHost(startDestination = "library")`. `library` → `LibraryScreen(onOpenFolders = { navigate("folders") }, onSongPlaying = { navigate("player") })`. `folders` → `FolderPickerScreen(onDone = { popBackStack() })`. `player` → `PlayerScreen(onBack = { popBackStack() })`. Set `setContent { DadMusicTheme { MainNavHost() } }` in `MainActivity`.

- [ ] **Step 2: Add release signing config**

In `app/build.gradle.kts`, add a `signingConfigs.release` block that reads `MUSICAPP_KEYSTORE_PATH`, `MUSICAPP_KEYSTORE_PASSWORD`, `MUSICAPP_KEY_ALIAS`, `MUSICAPP_KEY_PASSWORD` from `project.findProperty(...)`. Guard with a check: skip config block if any property is missing (so CI builds unsigned release artifacts for inspection).

- [ ] **Step 3: Build release APK**

Run: `./gradlew :app:assembleRelease`
Expected: `app/build/outputs/apk/release/app-release.apk` produced (unsigned if the keystore isn't configured on this machine; that's fine for M1).

- [ ] **Step 4: Write README install guide**

In `README.md`, document: how to generate a keystore, how to put credentials in `~/.gradle/gradle.properties`, how to build a signed release, how a non-technical musician enables "Install unknown apps" on Android to install the APK.

- [ ] **Step 5: End-to-end manual smoke on a real device**

Install the signed debug APK on the Xiaomi tablet. Pick a folder containing a few mp3s and one mp4 karaoke file. Confirm:
- Songs appear in the library list with sensible titles.
- Tapping an mp3 opens Player with controls and plays through aux cable.
- Tapping an mp4 opens Player in video mode and plays with lyrics visible.
- Rotating the tablet does not interrupt playback.
- Pulling the aux cable (audio becoming noisy) pauses playback.

Record findings in `docs/superpowers/manual-smoke-M1.md`.

- [ ] **Step 6: Commit**

```bash
git add app/ README.md docs/
git commit -m "feat: navigation + release signing + M1 manual smoke"
```

---

*End of Milestone 1 plan.*
