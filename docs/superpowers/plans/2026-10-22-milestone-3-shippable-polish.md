# Milestone 3: Shippable-APK polish

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Turn the M2 build — functional but still "development-grade" (destructive migrations, no first-launch flow, no in-app lyric fixes) — into an APK you can zip up and send to a friend without caveats. The core gig experience from M2 stays; M3 adds the paved-road for someone picking up the APK with no prior briefing.

**Architecture:** No new modules. Four focused areas: a real Room migration ladder (drops `fallbackToDestructiveMigration`), a 4-step onboarding wizard that owns the first launch, an `EntitlementSource` seam that keeps every v1.1 Pro-gated feature stubbed-but-wired, and a per-song lyric editor reachable from the Zen view. Light settings polish (About, auto-advance preference) rounds it out.

**Spec:** `docs/superpowers/specs/2026-10-21-dad-music-app-design.md` §5.2, §5.10, §5.11, §9.

## Global Constraints

- All M1/M2 constraints hold (min SDK 26, 100% Kotlin, SAF-only reads, dark-only theme, strings.xml, tap-target floors, no crash reporting SDK yet).
- **Migrations are now authoritative.** `fallbackToDestructiveMigration()` is removed. Every schema jump (v1→v2, v2→v3, v3→v4) ships with a tested `Migration` object. Future bumps follow the same pattern.
- **Entitlement checks run at feature entry, never on the hot path.** `entitlements.proEnabled` is read once when the gated UI mounts; recomposition shouldn't re-check it unless the source re-emits.
- **Onboarding runs exactly once.** Completion is persisted in DataStore. Re-launch after completion lands on the navigator; re-launch during onboarding resumes from step 1 (not partway through, to keep logic simple).
- **The sideload build shows no "Pro" branding.** The scaffold compiles, every Pro check returns true, no badge / countdown / upsell UI exists. We're planting the seam for v2 Play-Store work, not the UI.
- **Manual lyric edits are user-authored content.** They land with `source = USER_EDIT`, preempt a future LRCLIB re-fetch, and survive a rescan the same way sidecars do.

## Review Focus

1. **In-place upgrade from v1/v2/v3** — a user with an old build must land on v4 with their songs/lyrics/setlists/queue intact after the migrations run. Pinned to Task 2.
2. **Onboarding resumability** — killing the app mid-onboarding must not strand the user in a bad state. Pinned to Task 4.
3. **Empty-library onboarding** — if the user skips folder picking in step 2, step 3 (scan) must handle an empty folder set gracefully; step 4 shows the right summary. Pinned to Task 5/6.
4. **Pro-gate indirection** — flipping `SideloadEntitlementSource` → `PlayBillingEntitlementSource` in a future refactor must NOT require touching any feature code. Pinned to Task 1.
5. **Manual-edit vs scan race** — if the user edits lyrics and the user triggers a rescan at the same time, the user's edits win. Pinned to Task 8.

---

## Task 1: Entitlement scaffold

**Files:**
- Create: `entitlements/EntitlementSource.kt`, `entitlements/Entitlements.kt`, `entitlements/SideloadEntitlementSource.kt`, `di/EntitlementsBindsModule.kt`
- Test: `app/src/test/java/com/cameronbatten/dadmusic/entitlements/SideloadEntitlementSourceTest.kt`

**Interfaces:**
- `data class Entitlements(val proEnabled: Boolean)`
- `interface EntitlementSource { val entitlements: StateFlow<Entitlements> }`
- `@Singleton class SideloadEntitlementSource @Inject constructor() : EntitlementSource { override val entitlements = MutableStateFlow(Entitlements(proEnabled = true)).asStateFlow() }`

- [ ] **Step 1** — Write failing JVM test asserting `proEnabled == true` and the StateFlow emits once.
- [ ] **Step 2** — Run and verify fail.
- [ ] **Step 3** — Implement + bind.
- [ ] **Step 4** — Pass.
- [ ] **Step 5** — Commit: `feat(entitlements): sideload scaffold with proEnabled = true`.

---

## Task 2: Room migration ladder (Review Focus #1)

**Files:**
- Modify: `data/db/AppDatabase.kt` — drop destructive fallback and switch `exportSchema = true`. Keep `version = 4`.
- Modify: `di/AppModule.kt` — remove `fallbackToDestructiveMigration()`, add `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`.
- Create: `data/db/Migrations.kt` — three `Migration` objects.
- Create: `app/schemas/com.cameronbatten.dadmusic.data.db.AppDatabase/{1,2,3,4}.json` — exported via a one-off `./gradlew :app:kaptKotlin` with exportSchema on, then checked in.
- Modify: `app/build.gradle.kts` — pass schema export dir to KSP room-exporter.
- Test: `app/src/androidTest/.../data/db/MigrationTest.kt` — uses `MigrationTestHelper` to open each prior-version DB, apply the migration, verify shape.

**Interfaces:**
- `val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db) { db.execSQL("ALTER TABLE songs ADD COLUMN sizeBytes INTEGER NOT NULL DEFAULT 0") } }`
- `MIGRATION_2_3` — add `lyricsStatus`, `lyricsCachePath` columns + create `lyrics` table + its unique index.
- `MIGRATION_3_4` — create `setlists`, `setlist_entries`, `live_queue` tables + indices + FKs.

- [ ] **Step 1** — Export v1 schema JSON by temporarily reverting to version=1 and running `./gradlew :app:assembleDebug` with exportSchema on. Repeat for v2, v3. Check all four JSONs into `app/schemas/`.
- [ ] **Step 2** — Write `MigrationTest` that opens v1, runs v1→v2→v3→v4, inserts a song pre-migration, asserts it's still readable post-migration.
- [ ] **Step 3** — Implement migrations.
- [ ] **Step 4** — Flip `exportSchema = true` permanently, run migration test on-device, confirm green.
- [ ] **Step 5** — Commit: `feat(data): real migration ladder (drop destructive fallback)`.

---

## Task 3: First-launch detection

**Files:**
- Modify: `preferences/UserPreferences.kt` — add `onboardingCompleted: Flow<Boolean>` + `suspend fun markOnboardingComplete()`.
- Test: extend prefs test (if any) — otherwise rely on the onboarding smoke step.

- [ ] **Step 1** — Add the preference key + flow + setter.
- [ ] **Step 2** — Smoke-test manually that it persists across process death.
- [ ] **Step 3** — Commit: `feat(preferences): persist onboarding-completion flag`.

---

## Task 4: Onboarding — Welcome + routing (Review Focus #2)

**Files:**
- Create: `ui/onboarding/OnboardingNavHost.kt`, `ui/onboarding/WelcomeScreen.kt`, `ui/onboarding/OnboardingViewModel.kt`
- Modify: `MainNavHost.kt` — on launch, read `onboardingCompleted` once; if false, show onboarding; if true, show navigator.
- Test: `app/src/androidTest/.../ui/onboarding/WelcomeScreenTest.kt`

**Interfaces:**
- `OnboardingNavHost` owns an internal `NavController` scoped to steps: `welcome → folders → scanning → done`. "Done" calls `userPreferences.markOnboardingComplete()` then navigates up to the real navigator.
- `WelcomeScreen` has one sentence, one 64 dp "Let's go" button.
- `OnboardingViewModel` holds a `StateFlow<OnboardingStep>` so the host survives process death: if the step is anything other than `welcome`, re-launch during onboarding restarts at `welcome` (per spec simplicity rule).

- [ ] **Step 1** — Write failing test asserting the Welcome button fires the navigation callback.
- [ ] **Step 2** — Implement OnboardingNavHost + WelcomeScreen + MainNavHost branch.
- [ ] **Step 3** — Pass.
- [ ] **Step 4** — Commit: `feat(ui): onboarding wizard (step 1: welcome)`.

---

## Task 5: Onboarding — Folder picker (Review Focus #3)

**Files:**
- Create: `ui/onboarding/OnboardingFoldersScreen.kt`
- Reuse: `ActivityResultContracts.OpenDocumentTree`, `FolderRegistry` (via Hilt).
- Test: `app/src/androidTest/.../ui/onboarding/OnboardingFoldersScreenTest.kt`

**Interfaces:**
- Minimal screen. Hint text + "Pick folder" button + a list of already-picked folders + "Next" button.
- "Next" is enabled even with zero folders (user can finish onboarding then add folders later from the navigator). Scanning step handles the empty case gracefully.

- [ ] **Step 1** — Test: tapping "Next" with zero folders routes to Scanning step.
- [ ] **Step 2** — Implement.
- [ ] **Step 3** — Pass.
- [ ] **Step 4** — Commit: `feat(ui): onboarding step 2 (pick folders)`.

---

## Task 6: Onboarding — Scanning + Done

**Files:**
- Create: `ui/onboarding/OnboardingScanningScreen.kt`, `ui/onboarding/OnboardingDoneScreen.kt`
- Reuse: `LibraryImporter.importAll()` + `BulkLyricsFetchJob.run()`.

**Interfaces:**
- Scanning screen runs the importer then the lyrics fetch; progress UI chains both. Shows `Scanning files…` → `Fetching lyrics (N / M)…`.
- Zero-folder case: scan returns `ImportResult(0, 0, 0, 0)` instantly; proceeds to Done with the summary `"No folders yet — pick some from the folder icon when you're ready."`
- Done screen: summary counts + "Open my library" button → `markOnboardingComplete()` → navigate to the navigator.

- [ ] **Step 1** — Test: progress indicator advances with every song processed.
- [ ] **Step 2** — Implement both screens + chain.
- [ ] **Step 3** — Pass.
- [ ] **Step 4** — Commit: `feat(ui): onboarding steps 3+4 (scan progress + done)`.

---

## Task 7: Settings — About section + auto-advance preference

**Files:**
- Modify: `ui/settings/SettingsScreen.kt` — add About section (app name + version from BuildConfig + a one-line credit to LRCLIB) + an "After song finishes" dropdown (pause / auto-advance / repeat).
- Modify: `preferences/UserPreferences.kt` — add `onSongEnded: Flow<EndBehaviour>` with `enum class EndBehaviour { PAUSE, AUTO_ADVANCE, REPEAT }`. Default PAUSE for M3.
- Modify: `playback/PlaybackController.kt` — react to `onPlaybackStateChanged(STATE_ENDED)` by calling into a new `PlaybackCompletionHandler` that reads the preference and dispatches.
- Test: `app/src/androidTest/.../playback/PlaybackCompletionHandlerTest.kt`

**Interfaces:**
- `interface PlaybackCompletionHandler { suspend fun onSongEnded() }`
- Implementation reads the preference and the live queue (via `LiveQueueManager`) to pick an action.

- [ ] **Step 1** — Test the three behaviours against a seeded queue.
- [ ] **Step 2** — Implement.
- [ ] **Step 3** — Pass.
- [ ] **Step 4** — Commit: `feat(settings): About + after-song-ends behaviour`.

---

## Task 8: Manual lyric editor (Review Focus #5)

**Files:**
- Create: `ui/lyricseditor/LyricsEditorScreen.kt`, `ui/lyricseditor/LyricsEditorViewModel.kt`
- Modify: `ui/zen/ZenPlayerScreen.kt` — add an edit icon in the transport overlay. Taps route to `editor/{songId}`.
- Modify: `MainNavHost.kt` — add `editor/{songId}` route.
- Test: `app/src/androidTest/.../ui/lyricseditor/LyricsEditorScreenTest.kt`

**Interfaces:**
- `LyricsEditorViewModel` loads the existing `Lyrics` row (if any) and exposes its `rawLrc` as editable text.
- Save: parses the edited text via `LrcParser` to compute `hasTimings`, writes a new `Lyrics` row with `source = USER_EDIT`, updates the Song's `lyricsStatus` to `FETCHED_SYNCED` / `FETCHED_UNSYNCED` accordingly, and writes the cache file.
- Rescan protection: `LibraryImporter.importAll` checks `source == USER_EDIT` and skips the sidecar-adoption overwrite path for that song. (Add this guard in the importer.)
- Textarea gets `OutlinedTextField(minLines = 10)` and a monospace font so timestamps line up visually. Save / Cancel icons in the top bar.

- [ ] **Step 1** — Test: typing text + hitting save persists a Lyrics row with `source = USER_EDIT` and the correct `hasTimings`.
- [ ] **Step 2** — Test the rescan-protection branch: a USER_EDIT lyrics row is not overwritten even when a sidecar is present on the next scan.
- [ ] **Step 3** — Implement.
- [ ] **Step 4** — Pass.
- [ ] **Step 5** — Commit: `feat(ui): per-song manual lyric editor`.

---

## Task 9: Nav wiring + manual smoke + release

**Files:**
- Modify: `MainNavHost.kt`, `README.md`
- Create: `docs/superpowers/manual-smoke-M3.md`

- [ ] **Step 1** — Wire remaining routes: `editor/{songId}`.
- [ ] **Step 2** — Clear the app's data on the emulator, launch — must land on onboarding.
- [ ] **Step 3** — Walk the smoke doc end-to-end on the emulator; walk the physical-only items (reboot persistence, aux-cable) on the Xiaomi.
- [ ] **Step 4** — `./gradlew :app:assembleRelease`. Verify the APK is signed and installs on a device without MIUI warnings beyond the expected "unknown apps" prompt.
- [ ] **Step 5** — Commit: `feat: M3 wiring + release build + smoke doc`.

---

## Acceptance Criteria

M3 is done when, on a signed release APK installed on the Xiaomi tablet:

1. First launch after `pm clear com.cameronbatten.dadmusic` lands on the Welcome screen, not the navigator.
2. Walking onboarding (welcome → fold er pick with 1 folder → scan → done) ends on the navigator with the folder's setlist already populated.
3. Re-launching after onboarding opens straight into the navigator every time.
4. Installing a build from an older schema version (v1/v2/v3) in-place does NOT wipe the library — the Database Inspector shows all songs/lyrics/setlists intact post-migration.
5. Tapping edit on a Zen-view song, pasting LRC text, saving, and re-opening the song shows the edited lyrics scrolling.
6. A subsequent **Scan now** does not overwrite the edited lyrics.
7. Settings → After song ends → Auto-advance causes the next queue entry to start automatically when the current song finishes.
8. The build has zero `fallbackToDestructiveMigration()` references.

---

*End of Milestone 3 plan.*
