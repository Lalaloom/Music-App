# Milestone 6 — Bluetooth pedal manual smoke

Hardware-only: a Bluetooth HID page-turner (AirTurn, PageFlip, etc.) paired with the tablet.

- [ ] Settings → "Pedal mapping" opens the capture screen with the 5 actions listed, each "Not bound".
- [ ] Tap **Capture** next to "Play / pause" → "Press your pedal now" dialog appears.
- [ ] Press the pedal; the dialog closes; the row now reads "Bound to key N" where N is the pedal's keycode.
- [ ] Tap **Clear** → row goes back to "Not bound".
- [ ] Repeat for "Next song" and "Previous song".
- [ ] From the navigator, start playing a song and open the Zen view.
- [ ] Press the pedal bound to Play / pause → playback toggles.
- [ ] Press the pedal bound to Next → the next queue entry starts.
- [ ] Press the pedal bound to Previous → the previous queue entry starts.
- [ ] Scroll-up / scroll-down bindings land in the pedal event bus but are no-ops in the Zen view (future chord-sheet viewer wiring, M5-adjacent polish).
