# Pustaka Roadmap

_Last updated: 2026-11-17_

Pustaka is an offline EPUB reader for books that come in series of numbered volumes. Phases 0 to 7 took it from four screens of placeholder data to version 1.0.0, published on 2026-11-15. Phase 8 covers the fixes and improvements filed as [GitHub issues](https://github.com/naqibrafiqi/pustaka/issues) after using 1.0.0.

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
| D8 | Reader settings for each book, or one set for all | **One set for every book.** Phase 6's "Default reader settings" edits the same set. |
| D9 | How pages turn | **Paged by default**: swipe, or tap the left or right edge of the page. A **Scroll** switch in reader settings changes to continuous scrolling. |
| D10 | What happens at the end of a volume | The book is **marked Finished** and a card offers the **next volume** ("Read next" or "Close"). Nothing opens until you tap. |
| D11 | Where "Export as Markdown" sends highlights | The **Android share sheet**, with a `.md` file, so it can go to Drive, a notes app, email and so on. Nothing is written to the `Pustaka` folder. |
| D12 | What can be exported at once | **One book, or everything**: each book's group in the Highlights tab has Export, and the tab's menu has "Export all". Everything goes into one file, grouped by series and book. |
| D13 | Highlight colours | **Four: yellow, green, blue and pink.** Yellow is used for the very first highlight. |
| D14 | What "Highlight" does after selecting text | The text is **highlighted at once**, in the colour used last. Tapping the highlight lets you change its colour, add a note or delete it. |
| D15 | How a new library folder is chosen | Android's **system folder picker**, turned into a path such as `/storage/emulated/0/Download/Pustaka`, plus a "Use default folder" button. Only the phone's internal storage can be chosen. |
| D16 | What happens to books from the old folder after switching | They're **kept as missing**, with their progress and highlights. If the same files turn up in the new folder, they're matched by fingerprint and come back. |
| D17 | How search works | **One search screen**, opened from the search button in Library or Series, with results grouped into Series and Books. It matches name, author and genre. |
| D18 | Whether sort order and layout are remembered | **Yes.** The Series sort and layout and the Library grid/list choice are saved with the other settings. |
| D19 | Pustaka's own colour scheme | **Warm paper:** an amber-brown primary (`#8B5A2B`), cream surfaces and a muted green accent, with deep brown surfaces in dark mode. The app icon uses the same palette. |
| D20 | Whether dynamic colour is on by default | **Off.** Pustaka's own colours show out of the box, and wallpaper colours can be switched on in Settings (Android 12 and newer). |
| D21 | Who creates the release signing key | **The PM.** Claude provides the `keytool` command. The keystore and its passwords stay outside git, in a `keystore.properties` file the build reads. Claude never sees the passwords. |
| D22 | How 1.0.0 is published | **The PM publishes it.** Claude builds the signed APK and writes the release notes in the repo. The PM creates the GitHub Release and uploads the APK. |
| D23 | Which branch work goes into after 1.0.0 | **One branch per version.** `1.0.0` is released and stays as it is. The Phase 8 plan and the project skills are in `1.0.1`. The Phase 8 code goes into `1.1.0`, started from `1.0.1`. |

All the details are in [local_storage_and_library.md](local_storage_and_library.md).

### Open
These came up while planning Phase 8. Each has a proposal, which Claude follows unless you decide otherwise.

| # | Question | Proposal |
|---|---|---|
| D24 | Whether paragraphs are always justified (#32) | A **Justify** switch in reader settings and in Default reader settings, **on** by default. |
| D25 | What "latest book" means for the series cover (#38) | The **last volume in volume order**, not the most recently added file. Picking another volume's cover in Edit series overrides it. |
| D26 | When the "Back to where you were" button appears and disappears (#35) | It appears after jumping from the table of contents, the progress bar or a highlight. It goes away after reading **10 pages** forward from the new spot, after tapping it, or after closing the book. |
| D27 | Where bookmarks are listed (#37) | A **Bookmarks** tab in the Contents sheet, next to the chapters. Tapping one opens that page. |
| D28 | Whether to build the page-flip animation (#36) | Readium only slides or scrolls pages, so a page flip has to be built from scratch on top of it. Claude does a short **feasibility check first** and reports back before building it. |

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
- Check that Room 2.6.1 works with KSP 2.3.x once the first table exists, and upgrade to Room 2.7 or newer if it doesn't. ✅ It didn't: the 2.6.1 compiler fails under KSP2 ("unexpected jvm signature V") and its Gradle plugin can't load. Room is now 2.8.4, with the schema exported to `app/schemas/`.
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
Connect the app to real files, following [local_storage_and_library.md](local_storage_and_library.md). This phase is split into two pull requests:
1. The scanner and reading book details with Readium.
2. The permission and first-launch screens, and when scans run.

- **Permissions and first launch:**
  - A welcome screen that explains the "All files access" permission, then opens the system screen to turn it on. Android 10 uses the older storage permissions instead.
  - Create `Pustaka/` once access is granted. If access is refused, show a screen with a "Grant access" button.
  - ✅ Done in PR 2. Access is checked again whenever the app comes back, because it can be turned off in the system settings.
- **Scanner:**
  - Read `Pustaka/` one level deep: EPUBs directly inside become standalone books, folders become series (even if empty), and anything deeper or starting with `.` is ignored.
  - Only open files that are new or changed, judging by size and last-modified time.
  - Recognise moved or renamed books by fingerprint, so their progress is kept.
  - Mark books whose files have gone as missing, and restore them if the file comes back.
  - ✅ Done in PR 1, including series folders that are renamed.
- **Reading book details with Readium:**
  - Read title, creators with their roles, publisher, date, identifier, language, description, subjects and series position from `content.opf`.
  - Save the cover to the app's cache and count the total pages.
  - ✅ Done in PR 1 with Readium 3.3.0. Readium 3.4.0 needs Kotlin 2.4, and the project is on Kotlin 2.2, so upgrading is left for later.
- **Volume order:** the user's own number first, then the number in the EPUB, then a number in the filename, then the filename (10 after 9).
- **When scans run:** every time the app opens or comes back to the foreground, on pull-to-refresh, and from Settings. Scans run as a background job (WorkManager) and report a result like "3 new books found".
  - ✅ Done in PR 2. Settings only has the library section for now ("Library folder" and "Rescan library"). The rest of Settings comes in Phase 6.

**Done when:** you can copy a standalone EPUB and a folder of volumes into `Pustaka/`, pull to refresh, and see them with the right details and in the right order. Renaming one of the files keeps its progress.

### Phase 3: Connect the existing screens to real data (L)
Replace the placeholder data. This phase is split into two pull requests:
1. The four screens on real data, with their ViewModels, empty, loading and error states, and "More info". ✅ Done in PR 1.
2. Editing series and books, deleting, and "Create series". ✅ Done in PR 2.
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
The core feature, built on Readium's reader view. Readium's reader is an older Android Fragment, so it is wrapped for use in Compose. This phase is split into two pull requests:
1. Opening a book, page turning, the table of contents, the progress bar, and saving and resuming the position. ✅ Done in PR 1.
2. Reading sessions, reader settings, and finishing a book. ✅ Done in PR 2.

- Open a book from "Resume", "Continue From Page N" or "Read From Beginning".
- Page turning, a table of contents, and a progress bar.
- Save the reading position all the time and resume from it.
- Record reading sessions, which power "Read 32m ago", "Velocity" and "Estimated Left".
- Reader settings: font, text size, line spacing, theme (light, sepia or dark) and margins.
- Mark a book as finished automatically and offer to open the next volume.

**Done when:** you can read a volume from start to finish, with the position surviving app restarts.

### Phase 5: Highlights (M)
This phase is split into two pull requests:
1. Highlighting in the reader: select text to highlight it, see highlights on the page, and tap one to change its colour, add a note or delete it. Copy and Share are in the same selection menu. ✅ Done in PR 1.
2. The Highlights tab, opening the reader at a highlight, and exporting as Markdown. ✅ Done in PR 2.

- Select text in the reader to create a highlight, with an optional note and colour.
- **Highlights tab:** highlights grouped by series and book, with search. Tapping one opens the reader at that spot.
- Edit and delete highlights, and export them as Markdown.

### Phase 6: Settings, search and sorting (M)
This phase is split into two pull requests:
1. The Settings screen: library folder, hidden items, app theme, default reader settings, storage, and About with the open-source licences. ✅ Done in PR 1. The licences are listed by hand, because AboutLibraries needs Kotlin 2.4.
2. Search, sorting and layout in the Library and Series top bars. ✅ Done in PR 2.

- The Pustaka folder: change it (for example to `Download/Pustaka`) and rescan.
- Hidden items: a list of series and books removed from the library, with a button to restore each one.
- Search, sorting and layout: the search, sort/filter and grid buttons already in the Library and Series top bars. Search finds series and books by name, author and genre. Series can be sorted by name, recently added or recently read, and shown as cards or a compact grid. (Decided in Phase 3: the buttons were designed but not in any phase.)
- App theme (system, light or dark, with an option to use the phone's dynamic colours).
- Default reader settings.
- Storage usage and clearing the cover cache.
- About screen and open-source licences.

### Phase 7: Polish and release (M)
This phase is split into three pull requests:
1. Pustaka's colour scheme (D19, D20), the app icon, and a contrast check. ✅ Done in PR 1.
2. All text moved into the string resource file, and the accessibility pass. ✅ Done in PR 2. Text that ViewModels build is passed to the screens as `UiText` and looked up there. Preview sample data, log messages and library names in the licences list stay in code.
3. R8 code shrinking, UI tests for the main flows, the signed release build (D21) and the 1.0.0 release notes (D22). ✅ Done in PR 3. The release steps are in [RELEASING.md](RELEASING.md) and the notes in [releases/1.0.0.md](releases/1.0.0.md).

- Move all hard-coded text into the string resource file.
- Accessibility pass: content descriptions, touch targets, contrast.
- Pustaka's own colour scheme to replace the default purple template colours.
- UI tests for the main flows, and switch on code shrinking with R8 (keep rules already live in `rules.keep`).
- App icon, a signed release build, and version 1.0.0.

### Phase 8: Fixes and improvements after 1.0 (L)
Work from the GitHub issues filed on 2026-11-17. This phase is split into six pull requests, smallest first, all into the `1.1.0` branch (D23). The open decisions D24 to D28 apply.
1. **Reader fixes and layout (S–M):**
   - [#31](https://github.com/naqibrafiqi/pustaka/issues/31) (bug): the Contents sheet shakes when you swipe up on a list too short to scroll. A short list should stay still.
   - [#32](https://github.com/naqibrafiqi/pustaka/issues/32): justified paragraphs (D24). Readium only justifies when the book's own styling is off, so the switch has to work with the "Original" line-spacing option.
   - [#33](https://github.com/naqibrafiqi/pustaka/issues/33): space at the bottom of the page. Readium's margin setting only covers the left and right.
   - [#34](https://github.com/naqibrafiqi/pustaka/issues/34): centred chapter titles. Each book's own styling decides this, so Pustaka adds a small style rule of its own.
2. **Series cover from the latest volume (S + M):** [#38](https://github.com/naqibrafiqi/pustaka/issues/38). The series cover comes from the last volume instead of the first (D25), and Edit series lets you pick any volume's cover.
3. **Back to where you were (M):** [#35](https://github.com/naqibrafiqi/pustaka/issues/35). After a jump, the reader remembers the previous page and shows a button to go back to it (D26).
4. **Bookmarks (M):** [#37](https://github.com/naqibrafiqi/pustaka/issues/37). A bookmark button in the reader's top bar, a new database table, and a list of bookmarks (D27).
5. **Tablet layouts (M):** [#39](https://github.com/naqibrafiqi/pustaka/issues/39). Check every screen on a tablet, upright and sideways, and fix anything that overlaps, overflows or has text that's too small.
6. **Page-flip animation (L):** [#36](https://github.com/naqibrafiqi/pustaka/issues/36). A third page-turn choice in reader settings, next to Paged and Scroll. This starts with the feasibility check in D28.

**Done when:** every issue above is closed by a merged PR, or deliberately set aside with your agreement.

---

## Working agreement

- **Branches and pull requests:** one feature branch per phase, or per sub-feature for the large phases. Each starts from the version branch it's for (`1.1.0` for Phase 8, D23) and gets a PR into it for you to review. Claude creates each version branch locally from the one before it. Released versions such as `1.0.0` aren't changed. Version branches are merged into `main` separately.
- **Commit messages:** follow the existing style, e.g. `feat: …` and `fix: …`.
- **Reports:** at the end of each phase you'll get what was built, what was tested and how, and any decisions that came up along the way.
- **Scope changes:** if something in the designs is unclear or conflicts with this plan, Claude asks before building it.
- This file is the source of truth for scope and gets updated as phases are completed.

## Status

| Phase | Status |
|---|---|
| 0. Housekeeping | Done |
| 1. Data foundation | Done |
| 2. Library folder and scanning | Done |
| 3. Connect screens to data | Done |
| 4. Reader | Done |
| 5. Highlights | Done |
| 6. Settings, search and sorting | Done |
| 7. Polish and release | Done. 1.0.0 was published on 2026-11-15 (D22), following [RELEASING.md](RELEASING.md). |
| 8. Fixes and improvements after 1.0 | Planned for 1.1.0. Waiting on the open decisions D24 to D28. |
