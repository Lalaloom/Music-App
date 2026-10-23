# Milestone 4: Admin / override screen

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Give the user a hidden power-user panel that fixes gig-time problems without plugging the tablet into a laptop. Covers everything in spec §5.12 that is still relevant with sync descoped: entitlement override, force actions (rescan / clear lyrics cache / re-fetch LRCs), raw song DB editor, and a nuclear "reset device state" button. Logs export and sync inspector are deferred.

**Architecture:** One `AdminScreen.kt` with Compose sections, one `AdminViewModel.kt` that composes the existing collaborators (`EntitlementSource`, `LibraryImporter`, `BulkLyricsFetchJob`, `LrcCacheStore`, `SongDao`, `AppDatabase.clearAllTables()`). Entry point is a long-press on the About section header in Settings — matches the spec's "seven-tap-style, hidden by design" intent. A new `DebugEntitlementOverride` persists in DataStore and layers on top of `SideloadEntitlementSource`.

**Spec:** `docs/superpowers/specs/2026-10-21-dad-music-app-design.md` §5.12 (entries that survive the no-cloud-sync constraint).

## Global Constraints

- All M1/M2/M3 constraints hold.
- **Admin screen is device-local, no analytics event or crash report leaves the device**, matching spec §5.12 line 205.
- **No admin action is reversible by Undo.** Each destructive action requires an explicit confirmation dialog; the reset-device-state action requires two confirmations.
- **Admin entry point is undiscoverable by accident.** A long-press (≥ 500 ms) on the About *section header* opens it — not a tap, not a slow scroll, not a short press. A short press shows a toast confirming the row was tapped (so there's no silent "did it work?" ambiguity).
- **Entitlement override is sticky across process death**, so the user can QA Free-tier behaviour across cold starts without re-flipping it every launch.
- **Song DB editor edits must be preserved on rescan.** Rename detection in `LibraryImporter` uses `(sizeBytes + filename-fallback hash)` — user-edited titles / artists on a *confirmed* row survive a rescan. (Already covered by the M1 upsert path; this just codifies the invariant.)

## Review Focus

1. **Entry-point discoverability** — a non-technical musician must never trigger the admin screen by accident. Long-press-only, no visible affordance. Pinned to Task 1.
2. **Entitlement override round-trip** — flipping to `proEnabled = false` in admin must downgrade every gated feature on the next composition, flipping back must restore them, both must survive a process-death restart. Pinned to Task 3.
3. **Reset-device-state surgical scope** — the nuke button clears the DB + lyrics cache, but NEVER touches the user's SAF folder grants or the user's actual music files on disk. Post-reset, re-launch lands on the onboarding Welcome screen (not a broken navigator). Pinned to Task 6.
4. **Song DB editor concurrency** — an edit saves while a background rescan is running; the user's edit wins, not the scan's metadata re-read. Pinned to Task 5.
5. **Force-actions idempotency** — tapping "Rescan" or "Re-fetch LRCs" twice in quick succession must not double-schedule the job. Pinned to Task 4.

---

## Task 1: Admin entry point (Review Focus #1)

**Files:**
- Modify: `ui/settings/SettingsScreen.kt` — wrap the About `SectionHeader` in a `combinedClickable` that long-press-opens the admin route.
- Modify: `MainNavHost.kt` — add route `admin`.
- Modify: `app/src/main/res/values/strings.xml` — add `admin_screen_title`.
- Test: `app/src/androidTest/java/com/cameronbatten/dadmusic/ui/settings/SettingsScreenAdminEntryTest.kt`.

**Interfaces:**
- Long-press callback → `onOpenAdmin()` prop passed from `MainNavHost`.
- No visible indicator (no icon, no colour change).

- [ ] **Step 1** — Test: a short tap on About shows no navigation; a long press (≥ 500 ms) invokes the callback.
- [ ] **Step 2** — Implement.
- [ ] **Step 3** — Pass.
- [ ] **Step 4** — Commit: `feat(admin): hidden long-press entry from Settings → About`.

---

## Task 2: Admin screen scaffold

**Files:**
- Create: `ui/admin/AdminScreen.kt`, `ui/admin/AdminViewModel.kt`
- Modify: `strings.xml` — section headers.

**Interfaces:**
- `AdminViewModel @Inject constructor(...)` injects collaborators needed by downstream tasks (one list at a time per task); for Task 2 it only needs `EntitlementSource` to render the current `proEnabled` state at the top.
- `AdminScreen(onBack: () -> Unit)` renders a `Scaffold` + `TopAppBar` + scrolling `Column` with placeholder sections for Entitlement / Force Actions / Song DB / Reset — each a Material3 `ElevatedCard`.
- Danger-zone sections carry an outlined error-container background.

- [ ] **Step 1** — Pure scaffold with placeholder cards + nav wiring.
- [ ] **Step 2** — Smoke on emulator: long-press About → lands on admin, back-arrow returns to settings.
- [ ] **Step 3** — Commit: `feat(admin): screen scaffold with sectioned cards`.

---

## Task 3: Entitlement override (Review Focus #2)

**Files:**
- Create: `entitlements/DebugEntitlementOverride.kt`
- Modify: `entitlements/SideloadEntitlementSource.kt` — fold the override into its StateFlow.
- Modify: `preferences/UserPreferences.kt` — new boolean preference `adminProOverride` (null = passthrough; true/false = force).
- Modify: `ui/admin/AdminViewModel.kt` + `AdminScreen.kt` — render a tri-state chip row (Pro / Free / Default).
- Test: `app/src/test/java/com/cameronbatten/dadmusic/entitlements/EntitlementOverrideTest.kt` — JVM.

**Interfaces:**
- `class DebugEntitlementOverride @Inject constructor(prefs: UserPreferences) { val forced: StateFlow<Boolean?> }`
- `SideloadEntitlementSource.entitlements = combine(base, override) { b, o -> b.copy(proEnabled = o ?: b.proEnabled) }`
- `AdminViewModel.setOverride(Boolean?)` writes through to the preference.

- [ ] **Step 1** — Test the three states: null → passthrough, true → Pro, false → Free.
- [ ] **Step 2** — Implement.
- [ ] **Step 3** — Pass.
- [ ] **Step 4** — Smoke on emulator: flip override to Free, kill app, relaunch, confirm still Free; flip to Default, confirm Pro returns.
- [ ] **Step 5** — Commit: `feat(admin): persistent entitlement override`.

---

## Task 4: Force actions (Review Focus #5)

**Files:**
- Modify: `lyrics/LrcCacheStore.kt` — add `suspend fun clearAll(): Int`.
- Modify: `data/db/LyricsDao.kt` — add `suspend fun deleteAll(): Int`.
- Modify: `data/db/SongDao.kt` — add `suspend fun markAllLyricsStatusMissing()`.
- Modify: `ui/admin/AdminViewModel.kt` + `AdminScreen.kt` — three buttons:
  - **Rescan now** → `LibraryImporter.importAll()` with a `Mutex` guard to prevent concurrent runs.
  - **Clear lyrics cache** → confirm dialog → `LrcCacheStore.clearAll()` + `LyricsDao.deleteAll()` + `markAllLyricsStatusMissing()`.
  - **Re-fetch LRCs** → confirm dialog → calls into `BulkLyricsFetchJob.run()`.
- Test: `app/src/androidTest/.../lyrics/LrcCacheStoreTest.kt` — extend to cover `clearAll`.

**Interfaces:**
- `clearAll` scans the `lyrics/` directory and deletes every `*.lrc` plus any stale `*.lrc.tmp`; returns count deleted.
- All three force actions update a shared `isWorking: StateFlow<AdminWork?>` so the UI can disable the row and show a progress indicator — double-tap must be a no-op while a prior invocation is in flight.

- [ ] **Step 1** — Test `clearAll` returns the right count and leaves the directory empty.
- [ ] **Step 2** — Implement all three force actions with the mutex guard.
- [ ] **Step 3** — Smoke each on the emulator; confirm no crash, no duplicate progress bar.
- [ ] **Step 4** — Commit: `feat(admin): force actions (rescan / clear lyrics / re-fetch)`.

---

## Task 5: Song DB editor (Review Focus #4)

**Files:**
- Modify: `data/db/SongDao.kt` — add `fun observeAllByTitle(): Flow<List<Song>>` and `suspend fun updateTitleArtist(id, title, artist, confirmed)` (`@Query` with explicit `UPDATE`).
- Create: `ui/admin/SongEditorPanel.kt` (lives inside the admin package).
- Modify: `ui/admin/AdminViewModel.kt` + `AdminScreen.kt` — add a filter box + `LazyColumn` of rows; tap to expand → inline edit fields + confirmed checkbox + Save button.
- Test: `app/src/androidTest/.../data/db/SongDaoAdminTest.kt`.

**Interfaces:**
- `UPDATE` statement never touches `uri`, `sizeBytes`, `durationMs`, `fileType`, `lyricsStatus`, `lyricsCachePath`, `dateAdded`. User-authored edits only mutate `title`, `artist`, `confirmed`.
- Row body shows `uri` as grey helper text so the user can spot which file they're editing when titles collide.
- Save disabled while the field is unchanged from current state.

- [ ] **Step 1** — Test `updateTitleArtist` round-trips.
- [ ] **Step 2** — Implement the panel + VM wiring.
- [ ] **Step 3** — Smoke: edit a title, trigger a rescan, confirm title stays.
- [ ] **Step 4** — Commit: `feat(admin): raw song DB editor (title / artist / confirmed)`.

---

## Task 6: Reset device state (Review Focus #3)

**Files:**
- Modify: `data/db/AppDatabase.kt` — `fun resetAll()` wraps `clearAllTables()` + delete lyrics dir.
- Modify: `preferences/UserPreferences.kt` — add `suspend fun resetAllExceptFolders()` that clears every pref key *except* the SAF folder registry.
- Modify: `ui/admin/AdminViewModel.kt` + `AdminScreen.kt` — bottom-of-screen "Reset device state" button with a double-confirm dialog (first confirms intent, second requires typing "RESET" into a text field).
- On success: emit a one-shot event, navigate to `onboarding` with `popUpTo(0)`.

**Interfaces:**
- Reset must NOT touch:
  - SAF folder grants (`FolderRegistry` persisted URI permissions remain)
  - User's music files on disk
- Reset MUST clear:
  - `songs`, `lyrics`, `setlists`, `setlist_entries`, `live_queue`
  - All files under `filesDir/lyrics/`
  - All preferences *except* `folder_uris`
  - `onboarding_completed` → so the next launch runs the wizard.

- [ ] **Step 1** — Test the DB side: insert songs, call `resetAll`, assert tables empty.
- [ ] **Step 2** — Test the prefs side: write every preference, call `resetAllExceptFolders`, assert `folder_uris` survived and everything else is gone.
- [ ] **Step 3** — Implement + double-confirm dialog.
- [ ] **Step 4** — Smoke: on emulator, open admin → reset → confirm → confirm with "RESET" → lands on onboarding Welcome.
- [ ] **Step 5** — Commit: `feat(admin): reset device state with double-confirm`.

---

## Task 7: Nav wiring + manual smoke

**Files:**
- Modify: `MainNavHost.kt`
- Create: `docs/superpowers/manual-smoke-M4.md`
- Modify: `README.md` — add M4 smoke doc link.

- [ ] **Step 1** — Smoke checklist in `manual-smoke-M4.md` covering: discoverability (long-press only), entitlement override round-trip, each force action, DB editor edit survives rescan, reset returns to onboarding.
- [ ] **Step 2** — Install debug APK on emulator, walk the smoke doc.
- [ ] **Step 3** — Commit: `feat: M4 wiring + smoke doc`.

---

## Acceptance Criteria

M4 is done when, on the signed release APK installed on the Xiaomi tablet:

1. Short-pressing the About row does nothing; long-pressing it opens the admin screen.
2. Flipping the entitlement to Free in admin, killing the app, and re-launching keeps it on Free; flipping to Default restores Pro.
3. Admin → Rescan now completes with a progress indicator and does not duplicate rows in the library.
4. Admin → Clear lyrics cache empties `filesDir/lyrics/`, zeros the `lyrics` table, and resets `Song.lyricsStatus` for every row to `MISSING`.
5. Admin → Re-fetch LRCs re-populates lyrics on the same songs after the above.
6. Editing a title/artist in the Song DB editor saves and survives a subsequent rescan.
7. Admin → Reset device state → "RESET" lands on the onboarding Welcome screen with the previously-granted folders still registered (visible in step 2 of onboarding).
8. There is no visible admin entry point for a user who does not know about it.

---

*End of Milestone 4 plan.*
