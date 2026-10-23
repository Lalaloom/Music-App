# Milestone 3 — Manual Smoke Test Checklist

Signed-off checklist. Tick items with `[x]` as you verify them.

**Device:** Xiaomi tablet (primary) / emulator for parts an emulator can hit.
**Build:** signed release APK, Milestone 3.

---

## First launch (Review Focus #2, #3)

- [ ] `pm clear com.cameronbatten.dadmusic` on the device.
- [ ] Launch — lands on **Welcome** (not the navigator).
- [ ] Tap `Let's go` — lands on **Pick folders**.
- [ ] Tap `Pick a folder`, grant the SAF prompt, pick a folder with mp3s. Folder appears in the list.
- [ ] Tap `Next` — Scanning progress shows `Scanning files…` then `Fetching lyrics (N / M)`.
- [ ] Done screen reports `Added N songs.` and offers `Open my library`.
- [ ] Tap it — navigator opens, folder's setlist is populated.
- [ ] Kill the app from recents, relaunch — navigator opens straight (onboarding does **not** re-run).

## Empty-library onboarding (Review Focus #3)

- [ ] `pm clear` again. Launch, step through welcome, tap `Skip for now` on folders.
- [ ] Scanning returns instantly with 0 counts, Done shows the empty-library message.
- [ ] `Open my library` lands on the navigator; folder icon can still be used to add folders later.

## Migration ladder (Review Focus #1)

- [ ] Install the M1 (v1) debug build on a fresh profile, scan a folder, confirm library populates.
- [ ] Install the M2 (v3) build **in-place over the M1 build** — library survives, no crash on launch.
- [ ] Install this M3 (v4) build in-place over the M2 build — library, lyrics rows, setlists, and live queue all survive.
- [ ] Database Inspector confirms `songs`, `lyrics`, `setlists`, `setlist_entries`, `live_queue` tables are intact and populated.

## Settings — About + after-song-ends

- [ ] Settings shows an `About` section with app name, version from BuildConfig, and the LRCLIB credit line.
- [ ] `After song finishes` dropdown lists `Pause / Auto-advance / Repeat`. Default is `Pause`.
- [ ] With `Pause`: a song playing to completion stops; the next queue item does **not** auto-start.
- [ ] With `Auto-advance`: the next queue item starts automatically; the last entry stops naturally.
- [ ] With `Repeat`: the same song restarts from 0.

## Manual lyric editor (Review Focus #5)

- [ ] Open the Zen view on a song that has lyrics. Tap the screen → transport overlay appears, including a new pencil (Edit) icon alongside prev / play / next.
- [ ] Tap the Edit icon → lands on the lyric editor with the current `.lrc` text pre-filled in a monospace box; the top bar shows the song title.
- [ ] Edit a line, tap the check icon → returns to Zen; the edited line is now what scrolls.
- [ ] Kill the app, relaunch, re-open the song — the edit survives.
- [ ] Settings → `Fetch all missing lyrics` on a run that includes this song: the edited row is **not** overwritten (status stays `FETCHED_*`, source stays `USER_EDIT` in the DB inspector).
- [ ] Push a `.lrc` sidecar next to the file (different content) and `Scan now`: the edited row is **not** overwritten.
- [ ] On a song with no lyrics yet, Edit button opens an empty editor; pasting a block of plain lyrics saves successfully (status → `FETCHED_UNSYNCED`); pasting a block of `[mm:ss.xx]` lines saves as `FETCHED_SYNCED` and scrolls in the Zen view.

## Pro-gate scaffold (Review Focus #4)

- [ ] All v1.1-marked Pro features (none visible yet in M3, but any entitlement reads) behave as `proEnabled = true` — no upsell UI, no "unlock" badges anywhere.
- [ ] Toggling to a (future) `PlayBillingEntitlementSource` must not require touching feature code — confirmed by code review, not runtime.

## Release build sanity

- [ ] `./gradlew :app:assembleRelease` produces a signed APK (not `*-unsigned.apk`).
- [ ] `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` confirms a valid signature.
- [ ] Install over the previous MIUI build with the normal "unknown apps" prompt — no additional MIUI warnings.
- [ ] Zero references to `fallbackToDestructiveMigration` remain in the codebase (`grep -r fallbackToDestructiveMigration app/src`).

## Regressions from M1/M2

- [ ] Walk the M2 smoke doc end-to-end once more against this build — nothing from the previous milestone has regressed (navigator, drag reorder, request + undo, LRCLIB fetch, live-queue persistence, airplane-mode handling).

---

*Findings and issues (append inline):*

<!-- notes here -->
