# Milestone 1 — Manual Smoke Test Checklist

This file is a signed-off checklist. Append findings or discovered issues inline.
Submit this document (edited) back to the SDD workspace when complete.

**Device:** Xiaomi tablet  
**Build:** signed debug APK, Milestone 1

---

## Setup

- [x ] Install signed debug APK on Xiaomi tablet
- [ x] Grant storage access when prompted

## First launch

- [x ] Open app → see empty library with "Pick a folder" call-to-action

## Folder picking and scan

- [x ] Pick a folder on internal storage containing a mix of mp3s and one mp4 karaoke file
- [ x] Confirm scan progress indicator appears
- [ x] Confirm song count after scan matches the number of files in the folder
- [x ] Confirm songs appear in the library list with sensible titles (not raw filenames with underscores)

## Audio playback

- [ x] Tap an mp3 → Player opens in audio mode
- [x ] Title and artist are shown in the Player
- [x ] Play/pause button works
- [x ] Scrubber tracks playback position in real time
- [x ] Scrubber drag works smoothly (no mid-drag position snapping)
- [x ] Tap back → Library screen is still shown with songs listed

## Video playback

- [ x] Tap an mp4 → Player opens in video mode with PlayerView visible

## Device rotation

- [ x] Rotate device during playback → playback does not interrupt or restart

## Audio focus / noisy events

- [ ] Pull aux cable (or trigger audio-becoming-noisy event) → playback pauses automatically - Cant do due to using the emulator

## Error handling

- [ x] Try opening a file at a known-bad URI (renamed or deleted file) → error card appears instead of crash

## SD card (optional — if device has SD slot)

- [ x] Add an SD card folder → folder appears alongside internal folders in the folder list

## Persistence across reboot

- [ x] Reboot the tablet → open app → library is still populated (SAF URI permissions survived the reboot)

## Folder removal

- [x ] Remove (trash) a folder from the folder list → library updates to reflect the removal (songs from that folder no longer appear)

## UI quality checks

- [x ] Confirm no hardcoded English strings visible in the UI (quick scan of all visible screens)
- [x ] Confirm all primary tap targets feel comfortably large enough to tap with a saxophone in hand

---

*Findings and issues:*

<!-- Append notes here after testing -->
