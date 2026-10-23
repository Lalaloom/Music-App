# Milestone 8 — PDF viewer manual smoke

- [ ] Drop a `.pdf` into one of the registered folders and run Rescan from Settings or Admin.
- [ ] Navigator top bar has a new **PDF** icon; tapping it lands on the PDF list.
- [ ] The PDF appears with its filename-sans-extension as the title and the folder name as the subtitle.
- [ ] Tapping it opens the viewer with page 1 rendered via `PdfRenderer`.
- [ ] Top bar shows `Page 1 of N`; bottom bar previous/next buttons page through.
- [ ] At page 1, previous is disabled; at page N, next is disabled.
- [ ] Rotating the device re-renders at the new size (bitmaps are re-rendered on page change; rotation re-draws using the current bitmap at `ContentScale.Fit`).
- [ ] Deleting the file + rescan removes the row from the list.
