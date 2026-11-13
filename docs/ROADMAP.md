# Pustaka Roadmap

_Last updated: 2026-11-13_

Pustaka is an offline EPUB reader for books that come in series of numbered volumes. Today the app has its navigation and four designed screens (Library, Series, Series Details and Book Details), all showing placeholder data. This roadmap takes it from there to a release you can install and use every day.

**Roles:** the project manager (you) sets priorities and makes the product decisions. Claude does the engineering and reports at the end of each milestone.

---

## Decisions

### Decided
| # | Decision | Outcome |
|---|---|---|
| D1 | How the app gets access to the files | **"All files access"** (`MANAGE_EXTERNAL_STORAGE`). The app is distributed through GitHub, not the Play Store. The app creates `/storage/emulated/0/Pustaka` on first launch, and the user can change the location in Settings. |
| D2 | How EPUBs are read and displayed | **[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit)**: used for reading book details from `content.opf` (Phase 2) and for the reader (Phase 4). |
| D4 | Where book and series details come from | **Names come from the folder name (series) and the filename (book).** Other details (authors, illustrator, publisher, date, ID, description, subjects, cover) are read from `content.opf`. Everything can be edited, and edits are never written back into the files. |
| D6 | How the library is organised | The **user manages the folder by hand** and the app never renames or moves files. EPUBs directly in `Pustaka/` are standalone books. Each folder directly inside `Pustaka/` is a series, including empty ones. Only one level is scanned. |
| D3 | What "Offline Storage: X of Y files synced" on Series Details means | **"Indexed"**: the files the app has found and checked on disk. The label changes to "X of Y files indexed". There's no cloud sync in v1. |
| D5 | Languages | **English only** for v1. All text still moves into the string resource file (Phase 7), so a translation can be added later. |
| D7 | What "Remove from library" does | **Hides** the series or book so it doesn't come back on the next scan. It can be restored from Settings → Hidden items. "Delete folder/files" is offered as a separate choice. |

All the details are in [local_storage_and_library.md](local_storage_and_library.md). No decisions are open right now.

---

## Phases

Sizes: **S** is about one working session, **M** is 2–3 sessions, **L** is 4 or more. Each phase ends with a build that compiles and runs, and with a short report to you.

### Phase 0: Housekeeping (S)
Clean up the codebase before building on it.
- Confirm `./gradlew assembleDebug` succeeds. ✅ It already did. The Room version check moved to Phase 1, because Room's code generator doesn't run until the database has at least one table.
- Use the same version for every Kotlin plugin. ✅ The serialization plugin now uses the shared `kotlin` version (2.2.10).
- Delete the commented-out code in `PustakaRoutes.kt`.
- Make the back button on Series Details work. Its `onNavigateBack` is still a TODO.
- Save `README.md` as UTF-8 and give it a short project description.

**Done when:** the app builds cleanly and back navigation works on every screen.

### Phase 1: Data foundation (M)
Build the database and the layer that screens will read data through.
- Check that Room 2.6.1 works with KSP 2.3.x once the first table exists, and upgrade to Room 2.7 or newer if it doesn't.
- **Models and Room tables.** Book and series details keep two versions of each field: the value from the file or folder, and the user's edit if there is one.
  - `Series`: id, folder path, name (from the folder name / edited), author, illustrator, synopsis, main genre, tags, cover, hidden flag, and created/updated times.
  - `Book`: id, series id (empty for standalone books), file path, fingerprint, file size, last-modified time, status flags (missing, hidden), name (from the filename / edited), the details read from `content.opf` (title, creators with roles, publisher, date, identifier, language, description, subjects, series position), volume number (from the file / edited), cover, total positions, reading status (unread, reading or finished), date added, date last opened.
  - `ReadingProgress`: position in the book (a Readium-style locator), percent read, current page.
  - `ReadingSession`: start and end times and pages read. This feeds the "Velocity" and "Estimated Left" tiles.
  - `Highlight`: added now as a placeholder table and filled in during Phase 5.
- DAOs (database queries) that return Flows, plus repositories with interfaces, and Hilt modules to wire them up.
- DataStore to hold settings such as the chosen folder.
- Unit tests for the volume-order rules (number from the file, from the filename, and filename sorting with 10 after 9) and for the "value from file / user's edit" logic. Also tests for the DAOs.

**Done when:** the data layer is covered by tests and nothing in the UI changes yet.

### Phase 2: Library folder and scanning (L)
Connect the app to real files, following [local_storage_and_library.md](local_storage_and_library.md).
- **Permissions and first launch:**
  - A welcome screen that explains the "All files access" permission, then opens the system screen to turn it on. Android 10 uses the older storage permissions instead.
  - Create `Pustaka/` once access is granted. If access is refused, show a screen with a "Grant access" button.
- **Scanner:**
  - Read `Pustaka/` one level deep: EPUBs directly inside become standalone books, folders become series (even if empty), and anything deeper or starting with `.` is ignored.
  - Only open files that are new or changed, judging by size and last-modified time.
  - Recognise moved or renamed books by fingerprint, so their progress is kept.
  - Mark books whose files have gone as missing, and restore them if the file comes back.
- **Reading book details with Readium:**
  - Read title, creators with their roles, publisher, date, identifier, language, description, subjects and series position from `content.opf`.
  - Save the cover to the app's cache and count the total pages.
- **Volume order:** the user's own number first, then the number in the EPUB, then a number in the filename, then the filename (10 after 9).
- **When scans run:** every time the app opens or comes back to the foreground, on pull-to-refresh, and from Settings. Scans run as a background job (WorkManager) and report a result like "3 new books found".

**Done when:** you can copy a standalone EPUB and a folder of volumes into `Pustaka/`, pull to refresh, and see them with the right details and in the right order. Renaming one of the files keeps its progress.

### Phase 3: Connect the existing screens to real data (L)
Replace the placeholder data.
- Give each screen a ViewModel and a UI state type: Library, Series, Series Details and Book Details.
- **Library:** the "Currently Reading" card, "Recently Added", and the All / Currently reading / Unread filters with real counts.
- **Series:** the list of series, the volume preview, and "Continue reading".
- **Series Details:** header, synopsis, tags, reading statistics, storage (with the label changed to "X of Y files indexed"), and the volume list (with the grid/list toggle).
- **Book Details:** details, progress, stats tiles, and the More info / Edit details / Delete book menu.
- **Standalone books** shown in Library, but not in the Series tab.
- Screens to edit a series and a book: every field, including the name, can be edited and reset to its default, and a "Use details from file" button fills them from the EPUB.
- **Deleting:** choose between "Remove from library" and "Delete folder/files", with a confirmation showing how many files will be deleted. An empty series just has its folder deleted.
- **"Create series"** makes a new empty subfolder in `Pustaka/`.
- An empty state for each screen, plus loading and error states.

**Done when:** every screen that's already designed shows real data and no hard-coded sample text remains.

### Phase 4: Reader (L)
The core feature, built on Readium's reader view. Readium's reader is an older Android Fragment, so it is wrapped for use in Compose.
- Open a book from "Resume", "Continue From Page N" or "Read From Beginning".
- Page turning, a table of contents, and a progress bar.
- Save the reading position all the time and resume from it.
- Record reading sessions, which power "Read 32m ago", "Velocity" and "Estimated Left".
- Reader settings: font, text size, line spacing, theme (light, sepia or dark) and margins.
- Mark a book as finished automatically and offer to open the next volume.

**Done when:** you can read a volume from start to finish, with the position surviving app restarts.

### Phase 5: Highlights (M)
- Select text in the reader to create a highlight, with an optional note and colour.
- **Highlights tab:** highlights grouped by series and book, with search. Tapping one opens the reader at that spot.
- Edit and delete highlights, and export them as Markdown.

### Phase 6: Settings (S–M)
- The Pustaka folder: change it (for example to `Download/Pustaka`) and rescan.
- Hidden items: a list of series and books removed from the library, with a button to restore each one.
- App theme (system, light or dark, with an option to use the phone's dynamic colours).
- Default reader settings.
- Storage usage and clearing the cover cache.
- About screen and open-source licences.

### Phase 7: Polish and release (M)
- Move all hard-coded text into the string resource file.
- Accessibility pass: content descriptions, touch targets, contrast.
- Pustaka's own colour scheme to replace the default purple template colours.
- UI tests for the main flows, and switch on code shrinking with R8 (keep rules already live in `rules.keep`).
- App icon, a signed release build, and version 1.0.0.

---

## Working agreement

- **Branches and pull requests:** one feature branch per phase, or per sub-feature for the large phases. Each gets a PR into `main` for you to review.
- **Commit messages:** follow the existing style, e.g. `feat: …` and `fix: …`.
- **Reports:** at the end of each phase you'll get what was built, what was tested and how, and any decisions that came up along the way.
- **Scope changes:** if something in the designs is unclear or conflicts with this plan, Claude asks before building it.
- This file is the source of truth for scope and gets updated as phases are completed.

## Status

| Phase | Status |
|---|---|
| 0. Housekeeping | Done |
| 1. Data foundation | Not started |
| 2. Library folder and scanning | Not started (design agreed) |
| 3. Connect screens to data | Not started |
| 4. Reader | Not started |
| 5. Highlights | Not started |
| 6. Settings | Not started |
| 7. Polish and release | Not started |
