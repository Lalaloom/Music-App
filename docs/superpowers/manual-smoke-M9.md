# Milestone 9 — Band mode manual smoke

Hardware-only: two tablets on the same wifi. This milestone ships the networking scaffold; the host↔follower session plumbing (feeding `BandSyncManager.broadcast` from the real `PlaybackSession` + applying `incoming` on the follower) is M9.1 polish.

- [ ] Open the app on two devices.
- [ ] On device A: Settings → **Band mode (local network)** → **Host band session**. Status flips to "Hosting — bandmates on this wifi can join."
- [ ] On device B: Settings → **Band mode** → **Join band session**. Within a few seconds, device B should see device A under "Peers" as `DadMusic — <ip>:<port>`.
- [ ] Tap **Stop** on either side. Both devices return to Off.
- [ ] If both devices are offline from each other (different wifi), discovery times out silently; no crash.
- [ ] JVM test suite (`BandSyncProtocolTest`) exercises the wire format (encode/decode roundtrip, malformed-line rejection, URI quoting).

Known limitations (acknowledged, deferred to M9.1):
- Follower doesn't yet apply `incoming` to the local `PlaybackSession`.
- Host doesn't yet broadcast its own `PlaybackState` changes — the pipe is open, nothing flows through it.
- Mesh / leader election is not in scope.
