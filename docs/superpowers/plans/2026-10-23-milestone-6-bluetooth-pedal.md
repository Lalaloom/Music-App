# Milestone 6: Bluetooth pedal support

**Goal:** Lets a sax-and-sing performer step on an AirTurn / PageFlip HID pedal to advance to the next song, pause, scroll, etc. — hands stay on the horn.

**Architecture:** BT HID pedals present as hardware keyboards. We catch `KeyEvent`s at `MainActivity.onKeyDown` / `onKeyUp`, resolve the keycode through a user-configured `PedalBinding` list persisted in DataStore, and emit `PedalAction`s onto a process-wide `PedalEventBus` (shared flow, replay=0). ViewModels (`ZenPlayerViewModel` + future chord viewer) subscribe to the bus and route to playback / scroll APIs.

**Spec:** §6 v1.1 (Bluetooth pedal support).

**Tasks:**
- `PedalAction` enum + `PedalBinding` data class.
- `PedalBindings` object: encode / decode / lookup helpers (pure Kotlin).
- `PedalEventBus` singleton with `SharedFlow<PedalAction>`.
- Persistence: `UserPreferences.pedalBindingsText`.
- MainActivity: inject prefs + bus, collect bindings, intercept KeyDown.
- UI: `PedalBindingsScreen` with per-action capture + clear.
- Hook `ZenPlayerViewModel` to `PedalEventBus` for play/pause/next/prev.
- JVM test for `PedalBindings` encode/decode/lookup.

Hardware verification is in `docs/superpowers/manual-smoke-M6.md`.
