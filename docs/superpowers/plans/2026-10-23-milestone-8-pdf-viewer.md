# Milestone 8: PDF sheet music display

**Goal:** Discover `.pdf` files in registered folders, index them in the library, and render pages one at a time via Android's built-in `PdfRenderer`.

**Architecture:** `PdfSheet` entity + `PdfSheetDao` + migration 5 -> 6. Scanner already accepts `.pdf` (M5); importer now branches to `PdfSheetDao` for those files. `PdfViewerViewModel` opens the SAF URI through `ContentResolver.openFileDescriptor`, hands it to `PdfRenderer`, renders one page at a time to a Bitmap. `PdfViewerScreen` displays the current page with page-counter + prev/next.

**Spec:** §6 v1.1 (PDF sheet music display).

**Tasks:**
- `PdfSheet` entity + `PdfSheetDao`.
- `MIGRATION_5_6`.
- `LibraryImporter` picks up `.pdf` through a new `processPdfSheet` helper; prune pass handles orphaned PDFs.
- `PdfListViewModel` + `PdfListScreen` reachable from a new PDF icon in the navigator top bar.
- `PdfViewerViewModel` + `PdfViewerScreen` using `android.graphics.pdf.PdfRenderer`.

Manual smoke in `docs/superpowers/manual-smoke-M8.md`.
