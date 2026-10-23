# Milestone 4 — Manual Smoke Test Checklist

Signed-off checklist. Tick items with `[x]` as you verify them.

**Device:** Xiaomi tablet (primary) / emulator for parts an emulator can hit.
**Build:** signed release APK, Milestone 4.

---

## Entry point discoverability (Review Focus #1)

- [ ] Open **Settings**. Scroll to the About section. A short tap on the "About" header does nothing visible — no navigation, no toast.
- [ ] A long press (hold ≥ 500 ms) on the "About" header opens the **Admin** screen.
- [ ] No other screen (Navigator, Zen, Folders, Lyrics editor) exposes an entry point to the Admin screen.
- [ ] On the Admin screen, the system back button and the back arrow both return to Settings.

## Entitlement override round-trip (Review Focus #2)

- [ ] Admin → Entitlement → "Current: Pro" is shown by default; the **Default** chip is selected.
- [ ] Tap **Force Free**: the "Current" line flips to **Current: Free**.
- [ ] Kill the app from the recents tray and re-launch. Open Admin again — still **Current: Free**, **Force Free** still selected.
- [ ] Tap **Default**: back to **Current: Pro**. Kill-relaunch — still Pro.
- [ ] Tap **Force Pro** when **Default** was already Pro — no visible change; still Pro after kill-relaunch.
  (In v1.1+ Pro-gated UIs the Force Free flip should hide the gated features; those surfaces land in later milestones so there's nothing to tick in M4.)

## Force actions (Review Focus #5)

- [ ] Admin → Force actions → **Rescan now** runs with a progress bar and ends with a snackbar reporting added / updated / removed counts.
- [ ] Tapping **Rescan now** a second time while the first run is in flight shows the "Already running — hold on." snackbar; no double-scan happens.
- [ ] **Clear lyrics cache** shows a confirmation dialog. Confirming empties the `filesDir/lyrics/` directory (`adb shell run-as com.cameronbatten.dadmusic ls files/lyrics` returns nothing) and every song's `lyricsStatus` is back to `MISSING` (visible in the Database Inspector).
- [ ] **Re-fetch all lyrics** shows a confirmation dialog, then populates the cache again for songs LRCLIB knows about.

## Song DB editor (Review Focus #4)

- [ ] Admin → Song database → type a title fragment in the filter box; the list shrinks correctly.
- [ ] Tap **Edit** on a row — fields pre-fill, the SAF URI shows below as grey helper text.
- [ ] Change the title. **Save** enables. Confirm it. Snackbar reports "Song #N updated."; the row's title updates immediately.
- [ ] Admin → Force actions → **Rescan now**. After the rescan, the edited title is still the title (not the filename-derived one).
- [ ] Flip **Confirmed** off and on; Save enables / disables based on dirty state.
- [ ] An empty title disables Save (prevents an accidental blank).

## Reset device state (Review Focus #3)

- [ ] Admin → Danger zone → **Reset device state** opens an "Are you sure?" dialog.
- [ ] Confirm → a second dialog asks you to type **RESET**. The Reset button stays disabled until the typed text matches exactly.
- [ ] Type "reset" (lowercase) — button stays disabled.
- [ ] Type "RESET" — button enables; confirming lands you on the **Welcome** screen of onboarding.
- [ ] Walk onboarding → step 2 shows the folders you previously granted are still registered (no re-pick needed). (This proves SAF folder grants survived the reset.)
- [ ] After onboarding completes, the library is populated again from those same folders.
- [ ] Your source files on disk are untouched (verify from a different file manager app).

## Regressions

- [ ] Walk `docs/superpowers/manual-smoke-M3.md` once more — nothing from M3 regressed (onboarding, lyric editor, after-song-ends behaviour, migration preservation).

---

*Findings and issues (append inline):*

<!-- notes here -->
