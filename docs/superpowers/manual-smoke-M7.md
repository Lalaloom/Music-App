# Milestone 7 — Advanced lyric editor manual smoke

- [ ] From Zen → edit (M3 pencil icon) → simple editor opens.
- [ ] Simple editor's top bar now has an **Advanced** icon (tune icon); tapping it opens the advanced editor for the same song.
- [ ] Advanced editor loads the current `rawLrc` and splits it into editable rows (one per line), each showing its `mm:ss.xx` timestamp.
- [ ] Play button starts in-editor playback of the song. The playhead travels across the timeline strip.
- [ ] Tapping a line's clock icon stamps the current playback position as that line's timestamp.
- [ ] Tapping **Clear timestamp** on a timed line removes the stamp (the line becomes an untimed intro-style line).
- [ ] Insert / Remove line buttons work; new lines start untimed.
- [ ] Rewind-2s button jumps back 2 seconds and resumes playback — useful for retrying a tricky syllable.
- [ ] Save (check) persists USER_EDIT rows just like the simple editor. Reopening the song in Zen scrolls to the new timings.
- [ ] Rescan does not clobber the user-authored LRC (same contract as M3).

Live amplitude-based waveform rendering is deferred — the current strip is tick-marks + playhead. Note added to the roadmap.
