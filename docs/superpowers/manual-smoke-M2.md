# Milestone 2 — Manual Smoke Test Checklist

Signed-off checklist. Tick items with `[x]` as you verify them.

**Device:** Xiaomi tablet (primary) / emulator for parts an emulator can hit.
**Build:** signed debug APK, Milestone 2.

---

## Setup

- [ ] Install signed debug APK on the device. First launch should run the destructive migration from the M1 schema to the M2 v4 schema (empty library expected).
- [ ] Open Folders → pick a folder containing a mix of mp3s, one mp4, and at least one `.lrc` sidecar next to one mp3. Tap **Scan now**.
- [ ] Confirm summary says `Added N, updated 0, removed 0`.

## Navigator — setlist pane (left / Setlist tab)

- [ ] Dropdown opens and shows all scanned folders as setlists plus a top-of-list `Library` entry.
- [ ] Pick a setlist → queue populates in scan order.
- [ ] Pick `Library` → queue empties.
- [ ] Reopen the setlist → queue repopulates.

## Navigator — drag reorder

- [ ] Long-press the drag handle (right side of a row). Device buzzes briefly.
- [ ] Drag the row up or down. Other rows slide smoothly out of the way.
- [ ] Release — the row stays in the new slot.
- [ ] Backing out and re-opening the setlist shows the saved template unchanged (reorder lives in the live queue, not the saved setlist).
- [ ] `Save changes to <name>` button appears below the queue when the queue diverges from the parent setlist. Tapping it persists the reorder to the setlist. Reopen the setlist — new order is now the saved order.

## Navigator — search + request-add + undo (right / Library tab)

- [ ] Typing filters results by title.
- [ ] Typing filters results by artist.
- [ ] Clear button (X) resets the field.
- [ ] Tap a result → it appears at the end of the queue. Snackbar reads `"X" added to queue` with an `Undo` button.
- [ ] Tap **Undo** before 10 s → the row disappears from the queue.
- [ ] Wait 10 s without tapping → snackbar fades, row stays.
- [ ] Add two results rapidly → the first snackbar dismisses; only the latest add is undoable via snackbar.
- [ ] Tap a song already in the queue → brief `"X" is already in your queue` snackbar (no duplicate row).

## Playback hand-off

- [ ] Tap an mp3 row in the queue → opens the **Zen** view (black screen, three centred lines).
- [ ] Tap an mp4 row in the queue → opens the full-screen video player.
- [ ] Mini-player at the bottom of the navigator reflects the currently-playing song.
- [ ] Tapping the mini-player title opens the correct player (Zen for audio, video for mp4).

## Zen performance view

- [ ] Three lyric lines render: previous dimmed, current large/green, next dimmed.
- [ ] Lines advance in sync with the audio.
- [ ] Tap the screen → transport controls (play/pause/skip/scrubber) fade in; auto-fade after 3 s with no interaction.
- [ ] Play/pause button toggles playback.
- [ ] Scrubber drag works smoothly (no mid-drag snap).
- [ ] Back button (top-left chip) returns to the navigator.
- [ ] Playing a song with **no** lyrics shows the hint `"No lyrics yet. Settings → Fetch all lyrics…"`.
- [ ] Rotating the device during playback keeps playback going without a visible lyric jump.

## Sidecar `.lrc` adoption

- [ ] Push a `song.lrc` next to a `song.mp3` on the device. Scan again.
- [ ] Zen view of that song shows the sidecar's lyrics in sync.
- [ ] Database Inspector confirms the Song's `lyricsStatus = ADOPTED_FROM_DISK` and a `lyrics` row with `source = DISK_SIDECAR`.

## LRCLIB bulk fetch

- [ ] For a song with no sidecar, open Settings → tap `Fetch all missing lyrics`.
- [ ] Progress line updates `N / M (K failed)` while the fetch runs.
- [ ] If network is up and LRCLIB has the song, the Zen view of that song then shows timed lyrics.
- [ ] If network is down, every song ends with `lyricsStatus = FAILED` and the Zen view shows the no-lyrics hint. Re-running the fetch with network back succeeds.

## Settings

- [ ] Lyric offset slider moves in 100 ms steps across ±2 s. The label updates live.
- [ ] After committing an offset, the Zen view's current line shifts by that offset.
- [ ] Font size chips (Small / Medium / Large) change the current line's text size in the Zen view.

## Live queue persistence (Review Focus #3)

- [ ] Open a setlist, add a request, kill the app from the recents tray.
- [ ] Relaunch → navigator shows the queue restored exactly (including the request).

## Known-bad URI / airplane-mode (Review Focus #1 + #4)

- [ ] Delete a song file from disk, scan, open the Zen view of that song → error banner / graceful no-crash. (Row should already be gone from the library since the scan prune removes it.)
- [ ] Toggle airplane mode on → tap `Fetch all missing lyrics` → songs end at `FAILED`, no crash. Toggle airplane mode off → re-run fetch, same songs now succeed.

## UI quality

- [ ] No cut-off song titles in the queue rows (landscape or portrait).
- [ ] Play/pause button in the mini-player stays visible in both orientations.
- [ ] Zen view back-chip clear of the status bar / notch; transport bar clear of the system nav bar.

---

*Findings and issues (append inline):*

<!-- notes here -->
