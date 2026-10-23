# Milestone 9: Band mode (local-network sync)

**Goal:** Scaffold the local-network leader-follower sync between tablets on the same wifi. One host, N followers; host streams playback state updates, followers mirror.

**Scope choice:** This ships the networking scaffold (mDNS advertise + discover, TCP server + client, JSON-lines wire protocol, UI entry). Wiring the followers' `incoming` flow into `PlaybackSession` and feeding the host's broadcasts from the real `PlaybackState` is M9.1 polish — the two-device verification happens after that lands.

**Spec:** §6 v2 (Band mode — local-network leader-follower sync).

**Tasks:**
- `BandSyncMessage` sealed interface (Play / Pause / Heartbeat).
- `BandSyncProtocol` — hand-rolled JSON-lines codec (no external library). Correct behaviour on URL-quoting, malformed input rejection.
- `BandSyncManager` singleton — owns `NsdManager` registration + discovery, `ServerSocket` (hosting), `Socket` (joining), exposes `mode`, `peers`, `incoming`.
- `BandModeViewModel` + `BandModeScreen` with Host / Join / Stop + peer list.
- Settings has a "Band mode" section with a CTA.
- JVM test for the protocol (roundtrip, malformed, quoted strings).

Smoke doc `docs/superpowers/manual-smoke-M9.md` notes the known gaps for M9.1.
