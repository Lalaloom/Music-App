# Dad Music App — Design Document

**Date:** 2026-10-21
**Author:** Cameron (with Claude via /brainstorming)
**Status:** Draft, pending human approval

---

## 1. Intent & Why

A performance-grade music player for gigging musicians. Primarily a tool for playing backing tracks (mp3) and karaoke video (mp4) during live gigs, with auto-scrolling time-synced lyrics displayed on an Android tablet while the musician sings and plays saxophone.

The author plays sax and sings; he currently copies lyrics by hand into a different tool and manages his library in disk folders named "Set 1 / Set 2 / Set 3." Pain points this app resolves:

- Manual lyric copying → one-tap bulk fetch of time-synced lyrics
- Messy filenames (`Track 13.mp3`) with corrupted ID3 → automatic identification via filename / tag / audio fingerprint
- No fast way to handle special requests mid-gig → one-tap search + add to live queue
- No sync between tablet and laptop → manual-trigger sync via cloud (Google Drive, OneDrive) with a readable diff and merge/overwrite options
- No offline guarantee → fully offline-capable; cloud and YouTube are additive, never required

Secondary intent: once v1 is working, grow it into a market app that generates passive income under a freemium model, with chord sheets, PDF display, pedal support, and band-mode features reserved for later versions.

---

## 2. Goals

- Play mp3 and mp4 files from internal storage, microSD, and cloud (Drive, OneDrive).
- Auto-scrolling, time-synced lyrics on mp3 playback.
- Full-screen video playback for mp4 karaoke files (no lyric overlay — lyrics are baked in).
- Setlists (flat list, no gig-grouping) imported from folders. Each imported folder becomes one setlist, keeping the folder name.
- Live queue separate from saved setlist: special requests and reorders do not pollute the stored setlist unless explicitly saved.
- Manual sync button between devices; shows a human-readable diff; user picks "Merge both," "Use this device," or "Use other device"; 60-second undo after commit.
- Zero-paygate for core gig functionality in Free tier. 14-day trial of Pro on install.
- Shareable APK; each musician runs independent library and sync.
- Fully offline-functional (library, lyrics cache, setlists, playback, performance view). Cloud and YouTube degrade silently.

## Non-Goals (v1)

- No cross-user library sharing or collaborative setlists.
- No chord sheets, no PDF sheet music, no transposition (v1.1).
- No Bluetooth pedal / foot-switch support (v1.1).
- No band mode / multi-device leader-follower sync (v2+).
- No Dropbox (v2).
- No iOS, no desktop app (v2+ cross-platform via web if demand materializes).
- No Play Store submission (sideload APK only until the app proves stable with real gigging users).
- No marketing site or growth features (deferred).

## Anti-Goals (would make v1 a failure even if it works)

- Any paygate blocking core gig performance (play file, see lyrics, use saved setlist, manage live queue) during or after trial. The free tier must survive a gig forever.
- Any flow that requires network access to play a song and see its cached lyrics.
- Any onboarding step that would stump a non-technical musician (account creation with email verification, API key entry, manual config).
- Any sync behavior that silently overwrites data without user confirmation.
- Any destructive library action (rename, delete) that happens on disk without explicit confirmation.
- Any UI that cannot be read at a glance from several feet away with a sax in hand.

---

## 3. Constraints

- **Platform:** Android only in v1. Primary target device: Xiaomi tablet (~11" landscape). Must also work on Android phones.
- **Distribution:** APK sideload only in v1. Future Play Store release requires addressing lyric licensing, YouTube embed branding compliance, and privacy policy.
- **Offline:** Library playback + lyric display + setlist navigation + live queue must work with no network.
- **Non-technical users:** Target user is a gigging musician who is distracted, under time pressure, and does not read documentation. Big tap targets, undoable mistakes, zero jargon in user-facing text.
- **No subscription required for gig survival:** Trial is 14 days, after which Free tier retains all performance features.
- **Storage:** Android Scoped Storage. Reads via Storage Access Framework (SAF) for user-selected folders on internal, SD card, and cloud (via DocumentProvider).

---

## 4. User Personas

**Primary — Cameron (the author).**
Plays saxophone and sings. Uses mp3 backing tracks and mp4 karaoke videos. Builds setlists by dropping files into named folders ("Set 1, Set 2, Set 3") on a Xiaomi tablet. Reads lyrics off the tablet while singing. Takes requests mid-set and plays them "next or whenever convenient." Audio output usually via 3.5mm aux cable to a PA; sometimes other paths.

**Secondary — Shared musicians.**
Friends and local musicians Cameron will send the APK to. Different instruments (guitarists, keys, horn players). Some will stay on lyrics-only; others will press hard for chord sheets, transposition, and pedal support once they adopt the app. These shape v1.1 and v2.

---

## 5. Core Feature Set (v1)

### 5.1 Library management

- Import via Android SAF folder picker. Pick one or more folders on internal, SD, Drive, or OneDrive. Each selected folder becomes one setlist with that folder's name.
- Scoped Storage compliant — no permission to roam the device; only user-selected folders.
- Auto-detect pipeline for each file:
    1. Parse filename pattern: `"Title - Artist.ext"` and common variants.
    2. Read ID3 tags (mp3) and MP4 metadata atoms (mp4).
    3. **Audio files only:** if still unidentified, AcoustID audio fingerprint lookup (one-time network pass per file). Cached forever. Chromaprint does not reliably fingerprint karaoke video containers, so video files skip this step.
- Unidentified or ambiguous files land in a "Needs confirmation" pile. User reviews later with a two-tap confirm UI. Nothing on disk gets renamed.
- **Pre-existing LRC files are honored:** during scan, if a `<basename>.lrc` sits next to a music file, it is adopted as the song's lyric file and the LRCLIB fetch for that song is skipped. This respects the convention many musicians already follow.
- File type routing:
    - `.mp3`, `.m4a`, `.wav`, `.flac`, `.aac` → **audio mode** with lyric overlay.
    - `.mp4`, `.mkv`, `.webm` → **video mode** (full-screen playback, no overlay — lyrics are already baked into karaoke videos).

### 5.2 Lyrics

- Primary source: **LRCLIB** (free, no auth, actively maintained, returns LRC-format time-synced lyrics).
- Bulk fetch: during initial import and from a "fetch all missing" action in the library. Progress feedback.
- Fallback: plain lyrics (unsynced) if timed version unavailable. User can tap-time once to generate LRC via a dedicated recording screen (play the song, tap each line as it comes), or accept linear scroll: lines distributed evenly across song duration, with blank lines treated as half-weight gaps.
- Lyrics cached locally alongside song metadata. Offline-safe after first fetch.
- Manual lyric fix: a per-song editor to paste/edit lyrics and adjust timing (nudge +/- 500ms per line, or re-tap).

### 5.3 Performance view (mp3)

**Chosen: "Zen" layout.**

- Full-screen lyrics, black background, large text.
- Three lines visible at once: previous (dimmed), current (large, centered, highlighted), next (dimmed).
- Auto-scrolls with playback according to LRC timings.
- No visible controls by default.
- Single tap anywhere summons play / pause / skip / previous / scrubber. Controls auto-dim after 3 seconds of inactivity.
- Font size and line height configurable in settings (default: large, tuned for ~1 m viewing distance).
- Screen stays on during playback.

### 5.4 Performance view (mp4)

- Full-screen video playback via Media3 ExoPlayer.
- Tap to show the same overlay controls as audio mode (play/pause/skip/scrubber).
- No lyric overlay (lyrics are baked into the karaoke video).

### 5.5 Setlist navigator (between songs)

**Chosen: "Split view" layout.**

- Left pane: current setlist / live queue, with "NOW PLAYING" marker. Played songs dim with strikethrough. Drag to reorder.
- Right pane: full library and search. Drag or tap to add songs to the current queue.
- Dropdown at top of left pane switches between saved setlists (Set 1 / Set 2 / Set 3 / Library). **Library is a virtual view**, not a Setlist entity — it shows every song, searchable and sortable, with no stored order.
- Live-queue label and "Save tonight's changes to this setlist" button appear when the live queue diverges from its saved parent.
- Portrait orientation collapses to a single pane with a toggle; landscape (tablet default) shows both.

### 5.6 Live queue vs. saved setlist

- Each saved setlist is an immutable template until the user chooses to update it.
- When a user opens a saved setlist, the app creates a **live queue** that starts as a copy.
- All in-session changes (reorders, one-tap request adds, skips) mutate the live queue, not the saved setlist.
- A visible "Save changes to Set 1" button commits the live queue back to the saved setlist.
- Live queue persists across app restart (so a mid-set app crash doesn't lose state), but is discarded when the user explicitly chooses a different setlist.
- **Live queue is strictly device-local.** It is never synced to the cloud and never appears in another device's manifest. Only saved setlists sync.

### 5.7 Search / special requests

**Chosen: "One-tap add" layout.**

- Search field + microphone (optional voice-to-text) at top of the right pane.
- Results order: exact library matches first; fuzzy library matches second; YouTube results last (grayed when offline).
- Single tap on a library result → adds song to end of the live queue, with a 10-second undo toast.
- Long press on a library result → bottom sheet with "Play next / End of this set / Add to another set / Save to a specific setlist permanently."
- Single tap on a YouTube result → opens the in-app YouTube IFrame player with the song. Controls: back-to-navigator, play/pause.

### 5.8 YouTube integration

- **Pro tier (and 14-day trial):** In-app YouTube embed via official YouTube IFrame Player API (TOS-compliant: no download, no caching, YouTube branding visible per their requirements). YouTube results can be queued and behave exactly like native playback: can be "Up Next," can be skipped, auto-advance to the next queue entry when they finish.
- **Free tier (post-trial):** YouTube results in search show an "Open in YouTube app" button only. Tapping it hands off to the external YouTube app and pauses the live queue; the user manually comes back to the app to resume. No queue integration, no auto-advance.
- Grayed out and disabled when offline (both tiers).
- Lyrics overlay NOT drawn over YouTube video (most YouTube karaoke videos have lyrics burned in; also unclear licensing to overlay).

### 5.9 Sync (manual, user-triggered)

**Chosen: "Device picker" diff UI.**

- Sync button in the top navigation. Not automatic.
- On tap: app connects to the user's cloud (Drive or OneDrive), downloads the remote state manifest, compares to local.
- Shows a two-card diff screen: "This device" vs. "Other device," with human-readable change summaries ("+3 new songs," "Set 1 reordered," etc.).
- Three primary actions:
    - **Merge both (recommended):** apply all non-conflicting changes from both; last-write-wins per-record for conflicts.
    - **Use this device:** push local state as the source of truth, overwriting the remote.
    - **Use other device:** pull remote state, overwriting the local.
- After commit: 60-second undo toast. The pre-sync local state is held in a backup slot and can be restored.
- Music files themselves are synced via the user's native cloud client (Drive app, OneDrive app). The app only syncs metadata (song DB entries, setlists, lyrics, LRC files, user corrections, Pro status receipt).

### 5.10 Onboarding (4 steps)

**Chosen: 4-step wizard with folder picker.**

1. **Welcome** — one sentence, "Let's go" button. No account prompt.
2. **Where's your music?** — pick storage source (This tablet / Google Drive / OneDrive). Multi-select allowed. Each tap opens a folder-picker tree where the user ticks which folders to import; each selected folder becomes a setlist.
3. **Scanning** — progress bar with live counts (matched, lyrics found, needs confirmation).
4. **Done** — summary, "Open my library" button, note to review the "Needs confirmation" pile later.

No account creation, email, or password at any step. Cloud OAuth only prompted when a cloud option is selected on step 2.

### 5.11 Settings (minimal)

- Font size (3 presets).
- Lyric scroll offset (nudge global timing +/- milliseconds).
- Cloud accounts (connect / disconnect / change folder).
- Default behavior for "last played": pause, auto-advance, repeat.
- Pro status + unlock / restore.
- About, version, privacy policy, send feedback.

### 5.12 Admin / override screen (device-local)

Hidden power-user panel for inspecting and overriding app state when something goes wrong. Entry point: long-press the "About" row in Settings (seven-tap style, hidden by design so a non-technical user never stumbles into it).

Capabilities in v1:

- **Entitlement override**: toggle `proEnabled` manually. Lets the author force-test Free-tier behavior on a sideloaded build.
- **Sync state inspector**: view the raw local manifest, the remote peer manifests, and the computed diff. Download / email the state for debugging.
- **Force actions**: re-run full library scan, re-run AcoustID on unidentified files, clear lyrics cache, clear sync state, re-download LRC files.
- **Song DB editor**: raw table view of songs with inline edit — fix title / artist / fingerprint directly, mark as confirmed/unconfirmed.
- **Logs**: last 500 lines of app log, exportable to a file.
- **Reset device state**: nukes local DB and caches, keeps user files on disk and cloud settings intact. Requires double confirmation.

This is intentionally device-local in v1. A web admin console (remote user management, remote config, support ticket inbox) is v2+ scope and depends on whether the app ever has an identifiable user base — flagged as "monetization-adjacent" and deferred.

---

## 6. Deferred Features

### v1.1
- **Chord sheet display** (ChordPro format, plain text with chords above lyrics).
- **PDF sheet music display.**
- **Transposition** (up/down semitones for chord sheets).
- **AcoustID is in v1, not deferred.**
- **Bluetooth pedal support** (BT HID keycode mapping for standard page-turner pedals: AirTurn, PageFlip).

### v2
- Dropbox sync.
- Band mode (local-network leader-follower sync between tablets).
- Shared setlists (export / import between musicians).
- iOS build (likely via a shared Kotlin Multiplatform core if the Android app validates product-market fit; otherwise native Swift).
- Play Store submission.
- Marketing site.
- Advanced lyric editor with waveform display.

---

## 7. Technical Architecture

### 7.1 Stack

- **Language:** Kotlin (100%)
- **UI:** Jetpack Compose
- **Minimum SDK:** API 26 (Android 8.0) — covers 97%+ of active Android devices.
- **Target SDK:** latest stable at build time.
- **Media:** AndroidX Media3 (ExoPlayer successor) for both audio and video. Native karaoke mp4 support, hardware-accelerated.
- **Database:** Room (SQLite) for library, setlists, lyrics cache, sync manifest.
- **Networking:** OkHttp + Retrofit + kotlinx-serialization for cloud and LRCLIB APIs.
- **Dependency injection:** Hilt.
- **Async:** Kotlin Coroutines + Flow.
- **Crash reporting:** Sentry Android SDK (free tier).
- **Build:** Gradle Kotlin DSL. GitHub Actions for CI if the repo lands on GitHub.
- **No analytics in v1.** Add PostHog or similar at 10+ users.

### 7.2 Data model (Room entities, abbreviated)

```
Song { id, title, artist, album, filePath, fileType, durationMs,
       acoustIdFingerprint, musicBrainzId, lyricsStatus, lyricsSyncedPath,
       dateAdded, confirmed, sourceStorage, modifiedUtc, deviceId }

Setlist { id, name, sourceFolder?, createdAt, modifiedAt, orderIndex,
          modifiedUtc, deviceId }

SetlistEntry { setlistId, songId, position, modifiedUtc, deviceId }

LiveQueueEntry { position, songId, source(saved|request), parentSetlistId }
    -- device-local only; never synced

SyncManifestEntry { entityType, entityId, lastModifiedUtc, deviceId,
                    hash, deleted }

CloudAccount { provider, accountEmail, rootFolderId, lastSyncUtc,
               lastSyncResult }
```

- Each synced entity carries `modifiedUtc` + `deviceId`, enabling per-record last-write-wins.
- `SetlistEntry.position` uses sparse integers (initial values 1000, 2000, 3000…). New insertions take midpoints (e.g., 1500 between 1000 and 2000). Periodic re-numbering is cheap when gaps shrink. This makes reorder-vs-insert conflicts tractable: the merge keeps all entries from both sides and resolves overlapping positions by last-writer.
- **Song identity across devices:** songs resolve by AcoustID fingerprint when available, otherwise by a normalized `(title, artist, durationMs ± 2000ms)` triple. Videos without reliable fingerprints resolve by the normalized triple only.
- **Dangling setlist entries** on a device that received a sync but doesn't own the referenced song: shown as a greyed-out "Not in your library — tap to locate" placeholder. On next library scan, auto-match by the triple above; a successful match attaches the row. No automatic deletion.
- Deletions are tombstones (soft delete), kept until two sync rounds confirm propagation, then purged.
- Setlists sync as metadata. Music files themselves live in the user's own cloud folder structure and are not transferred by the app.

### 7.3 Sync protocol

- Each device writes its state as a single `manifest.json` file to a dedicated folder in the user's cloud (`/DadMusicApp/sync/<deviceId>-manifest.json`), plus a `lyrics/` subfolder for per-song LRC files.
- On sync: pull all peer manifests, compute diff vs. local, present the device-picker UI.
- "Merge both" applies peer changes per the chosen conflict rule (last-write-wins per record by `modifiedUtc`), then uploads a new local manifest.
- "Use this device" uploads local manifest and marks peer entries as superseded.
- "Use other device" overwrites local DB with the peer's state. Local backup held for 60s undo window.

### 7.4 Cloud auth

- Google Drive: OAuth 2.0 via Google Sign-In. App-specific scope (`drive.file`) — only touches files the app itself creates. User does NOT grant access to their whole Drive.
- OneDrive: Microsoft Identity Platform (MSAL). Scoped to a specific app folder.
- Client secrets: tablet-side OAuth clients can safely ship their client IDs. No client secrets stored in the APK (OAuth code-flow with PKCE).

### 7.5 Audio sync precision

- LRC timings are typically accurate to ~100ms. ExoPlayer position polling at 60Hz is more than enough.
- 3.5mm cable output (user's primary path) is sample-accurate, no concern.
- Bluetooth output can introduce 100–300ms audio latency; a global "lyric offset" setting lets the user nudge timings per output path.

### 7.6 AcoustID / MusicBrainz

- Fingerprinting via Chromaprint (bundled native library). Audio files only; video files skip this step entirely.
- Lookup against the AcoustID web service with the author's non-commercial API key baked into v1 (acceptable for a friends-only sideload). Commercial license required before Play Store monetization — flagged under R2.

### 7.7 Lyrics

- LRCLIB `/api/get` endpoint by (title, artist) or by MusicBrainz ID when available.
- Returned LRC files stored verbatim in `lyrics/<songId>.lrc` and referenced by the Song entity. Human-readable, user-editable.

---

## 8. UX Principles

- **Big tap targets.** Minimum 48dp hit area; preferred 64dp for primary actions during a gig.
- **Undo everything destructive** within 60 seconds (sync, delete, bulk edits). Shorter 10-second undo for one-tap adds.
- **Zero jargon user-facing.** Any error message uses plain English. No "HTTP 401" ever surfaces.
- **Dark theme by default** (easier on eyes in dim venues, better for OLED battery).
- **Screen stays on during playback.** User can override.
- **Haptic feedback on primary actions** (song added, sync committed) — subtle, confirms the tap without needing to look.
- **Portrait and landscape both supported.** Landscape is the primary layout for the Xiaomi tablet.
- **Smooth animations and transitions throughout.** Not decorative — animations communicate state change and spatial relationship. Specific motion requirements:
    - **Lyric line advance:** current line scales up and fades to full opacity while the previous line dims and shrinks; 250 ms ease-out curve. No jarring snap on each line.
    - **Playback controls:** fade in on tap (150 ms), fade out after 3 s of no interaction (250 ms).
    - **Setlist reorder:** drag item lifts with a shadow and scale-up (150 ms); other items shift out of the way (250 ms ease-in-out).
    - **Screen transitions:** slide + fade between navigator and performance view (300 ms); shared-element transition for the "now playing" card.
    - **Sync diff screen:** cards slide in from either side; chosen action pulses on tap, then screen slides out to a progress indicator.
    - **Toasts (undo, status):** slide up from bottom with spring curve; auto-dismiss slides down.
    - **Song add to queue:** the tapped result shrinks and flies to the queue, where the new entry briefly highlights.
    - **Progress bar:** linearly interpolated against position polling, no stepping.
    - All animations respect the system "Reduce motion" accessibility setting and fall back to crossfades when enabled.
- **Material Design 3 motion defaults** (Jetpack Compose provides these out of the box); custom curves and durations only where the above list calls them out.

---

## 9. Monetization

Model: **Freemium with 14-day Pro trial** — plumbed from v1, activated at v2.

### v1 (sideloaded APK) — entitlement scaffold with everything unlocked

- Full feature-flag system in code: every Pro-gated feature checks `entitlements.proEnabled` before executing.
- A single `EntitlementSource` interface. v1 ships a `SideloadEntitlementSource` implementation that returns `proEnabled = true` always.
- No trial countdown UI, no upsell UI, no "Pro" badges visible to sideload users. Everything just works.
- No Google Play Billing integration (would require Play Store registration anyway).
- Result: friends get all features free forever on the sideload build; the plumbing is already in place for v2.

### v2 (Play Store release) — flip the entitlement source, add billing

A thin refactor swaps `SideloadEntitlementSource` for `PlayBillingEntitlementSource`:

- Google Play Billing integration for subscriptions and one-time purchases.
- 14-day free Pro trial on first install.
- Trial countdown and upgrade UI appear once the entitlement source signals the trial is active or expired.
- Pro badges become visible in the UI where Pro features gate.

No refactor of feature code is required — all Pro gates already exist and compile; only the source of truth for the flag changes.

**Free tier (post-trial)** — the floor is "gig survives without Pro":

- Unlimited local library import.
- Full performance view, lyrics, auto-scroll, setlists, live queue.
- Manual sync to one cloud account.
- YouTube hand-off (external launch to YouTube app).
- Full offline support.

**Pro tier:**

- In-app YouTube embed with queue integration.
- AcoustID auto-match for messy filenames.
- Bulk LRCLIB fetch across the whole library in one shot.
- Multiple simultaneous cloud accounts (Drive AND OneDrive together).
- Chord sheets + PDF sheet music + transposition (v1.1 features become Pro at launch).
- Bluetooth pedal support (v1.1).
- Band mode (v2).

**Proposed pricing** (confirm at launch):

- Monthly: $3.99/mo
- Yearly: $29/yr
- Lifetime one-time: $59

---

## 10. Known Risks

### R1 — Lyrics licensing (critical for monetization)
LRCLIB is free and legally gray; Google has removed lyric apps from the Play Store in the past. Mitigation path: v1 ships to friends; before Play Store release, migrate to a licensed source (Musixmatch SDK has free + commercial tiers) OR restrict in-app lyric fetch to a "user supplies their own LRC file" model, OR secure a direct license.

### R2 — AcoustID commercial license
AcoustID / MusicBrainz usage is free for non-commercial. Monetized use requires a commercial license (currently reasonable: tens of USD per month at modest scale). Budget for it.

### R3 — Google / Microsoft API quotas and OAuth verification
Both Drive and OneDrive OAuth require app verification before publishing to a wide audience. The sideloaded APK side-steps this for v1 (OAuth consent screen will show an unverified warning that the author's friends can click through). Pre-Play-Store, apply for verification.

### R4 — Android Scoped Storage edge cases
Writing user-selected content requires SAF. Some Xiaomi MIUI builds have quirky SAF behavior; test on the author's actual device early.

### R5 — Sync conflict edge cases
Last-write-wins is simple but can lose intentional edits in rare races. Mitigation: the 60-second undo plus a persistent backup slot ("Previous sync state"), restorable from Settings.

### R6 — YouTube IFrame Player branding and policy
The YouTube Player API has strict visual requirements (must show YouTube branding, must not simulate audio-only). Design the in-app embed to comply from day one.

### R7 — Battery during long gigs
Screen-always-on + continuous playback + Bluetooth = material battery draw. Document in-app that a charger is recommended for sets longer than 90 minutes.

---

## 11. What's Left to the Builder

The following are explicit builder-call items, not open design questions:

- Exact font family and typographic scale (choose a highly legible sans-serif, test under stage lighting).
- Precise colors beyond "black background, high contrast text" (choose a comfortable off-white and a single accent color for highlights, confirmable in a quick palette review).
- Internal module structure of the Kotlin codebase (follow standard Android app architecture: `data`, `domain`, `ui`, feature modules).
- Choice of specific OAuth library per cloud provider (use whatever is officially supported).
- Decision on whether to ship an in-house ChordPro parser or use an existing OSS one (defer; v1.1 scope).
- Exact wording of all error messages, onboarding copy, and marketing copy (hold for a copy pass before beta).

---

## 12. Builder Calls Settled During Review

The design-review pass surfaced several places where a builder would have to guess. The following are settled in-document; marking them here so the author can push back during approval:

- **5.1** — AcoustID runs on audio files only; videos skip fingerprinting and fall back to filename/tag.
- **5.1** — Pre-existing `.lrc` files on disk are honored during scan and preempt LRCLIB fetch.
- **5.2** — Linear-scroll fallback = even distribution of lyric lines across song duration, blank lines half-weight. Tap-time is a dedicated screen, not a live-performance feature.
- **5.5** — "Library" in the setlist dropdown is a virtual view, not a Setlist row.
- **5.6** — Live queue is strictly device-local; never syncs.
- **5.8** — Free tier YouTube = external hand-off only (no queue integration). Pro tier = in-app embed with full queue integration.
- **7.2** — `SetlistEntry.position` uses sparse integers to make reorder-vs-insert conflicts tractable; merge keeps all entries from both sides, last-writer resolves position collisions.
- **7.2** — Song identity across devices falls back to normalized `(title, artist, duration±2s)` when no AcoustID is available. Dangling entries become greyed "Not in your library" placeholders that auto-resolve on the next scan.
- **7.6** — v1 ships with the author's non-commercial AcoustID key baked in; swap to a commercial license before Play Store release (already captured as R2).

---

*End of document.*
