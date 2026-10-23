# Milestone 7: Advanced lyric editor

**Goal:** Line-by-line editing with in-editor playback and tap-to-time. Users can hear the track and stamp timestamps as the beat lands, instead of hand-counting `[mm:ss.xx]` markers.

**Scope choice:** Full amplitude-based waveform rendering (MediaCodec decode → RMS buckets) is deferred to a v2.1 polish. M7 ships a tick-mark timeline + playhead, which is enough to anchor tap-to-time against while keeping the implementation to one ViewModel.

**Spec:** §6 v2 (Advanced lyric editor with waveform display).

**Tasks:**
- `AdvancedLyricsEditorViewModel`: owns an `ExoPlayer`, loads existing LRC, parses to `EditableLine`s, exposes `stampLineAtCurrentPosition` / `clearLineStamp` / `insertLineAfter` / `removeLine`, and builds LRC output.
- `AdvancedLyricsEditorScreen`: timeline strip (tick marks + playhead), play/pause + rewind-2s controls, per-line row with stamp button + text field + insert / remove.
- Simple `LyricsEditorScreen` gets a `Tune` icon in its top bar that routes to the advanced editor for the same song.
- Nav: `editor-advanced/{songId}` route.
- JVM test: `AdvancedLyricsEditorViewModel.buildLrcText` emits valid LRC for both timed and untimed lines (centisecond truncation).

Hardware verification in `docs/superpowers/manual-smoke-M7.md`.
