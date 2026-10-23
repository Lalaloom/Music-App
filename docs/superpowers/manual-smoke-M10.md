# Milestone 10 — Shared setlists manual smoke

- [ ] Settings → **Share setlists (export / import)** opens the sharing screen.
- [ ] Local setlists appear with an export arrow next to each.
- [ ] Tap export on a setlist → system file picker (CreateDocument) appears → save as e.g. `set-a.setlist.json`.
- [ ] Open the saved file in a file manager: it is a well-formed JSON with `name` and `songs: [{title, artist?}, …]`.
- [ ] Share the file (email / Drive / airdrop) to a second device.
- [ ] On the second device: Share setlists → **Import a .setlist.json** → system file picker opens → pick the file.
- [ ] A new setlist with the same name appears in the setlist dropdown.
- [ ] Songs that match (by title + artist, case-insensitive) are added in order.
- [ ] Missing songs trigger the "Some songs were not matched" dialog listing up to 8 titles.
- [ ] JVM test suite (`SharedSetlistCodecTest`) covers encode/decode roundtrip, missing artist, quoted / newlined text, empty setlist, malformed input.

Matching is intentionally conservative — fuzzy / fingerprint-based matching (deferred to AcoustID in a later milestone) is not in M10.
