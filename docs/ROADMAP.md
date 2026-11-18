# Pustaka Roadmap

_Last updated: 2026-11-18_

Pustaka is an offline EPUB reader for books that come in series of numbered volumes. Phases 0 to 7 took it from four screens of placeholder data to version 1.0.0, published on 2026-11-15. Phase 8 covered the fixes and improvements filed as [GitHub issues](https://github.com/naqibrafiqi/pustaka/issues) after using 1.0.0, and became 1.1.0, published on 2026-11-17. Phase 9 covers the issues filed after using 1.1.0, for 1.2.0.

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
| D24 | Whether paragraphs are always justified (#32) | A **Justify** switch in reader settings and in Default reader settings, **on** by default. Built as proposed in part 1 and approved with it. |
| D25 | What "latest book" means for the series cover (#38) | The **last volume in volume order**, not the most recently added file. Picking another volume's cover in Edit series overrides it. Built as proposed in part 2 and approved with it. |
| D26 | When the "Back to where you were" button appears and disappears (#35) | It appears after jumping from the table of contents, the page slider or a highlight, and **shows and hides with the reader's bars**. It goes away after **10 screen turns** forward from the new spot (pages scrolled, when scrolling), after tapping it, on turning back to that page, or after closing the book. Approved with part 3, with the turns and bars changes from review. |
| D27 | Where bookmarks are listed (#37) | A **Bookmarks** tab in the Contents sheet, next to the chapters, with a bookmark button in the reader's top bar. A bookmark belongs to a page number, and tapping one opens that page. Built as proposed in part 4 and approved with it. |
| D28 | Whether to build the page-flip animation (#36) | **Set aside for a later version.** A quick trial showed that a page curl can be drawn over Readium's page, turned by a tap or by dragging with a finger, but it was far from what you have in mind. #36 stays open and comes back in a later version. The trial is kept on the local branch `spike/page-flip-d28` for reference and isn't merged. |
| D29 | Where the tabs go on a tablet (#39) | On a window **600dp or wider** (tablets, unfolded foldables, phones on their side), the tabs are in a **navigation rail** down the left side, kept on every screen. Phones held upright keep the bottom bar. Built in part 5 and approved with it. |
| D30 | Whether the reader's **Auto** theme stays when Dark grey is added (#55) | **Keep Auto.** It's the default and follows the app's light or dark theme, so removing it would change the reader for anyone who never picked a theme. Dark grey is added after Dark: Light, Sepia, Dark, Dark grey. Auto keeps using Dark in dark mode. Approved with the Phase 9 plan. |
| D31 | What brightness the reader uses (#54) | Android can't pause auto-brightness for a single app, only replace it, so the reader sets its own brightness. A **Brightness** slider in reader settings, and in Default reader settings. The first time, it starts at the phone's brightness at that moment. The phone's own brightness and auto-brightness come back as soon as you leave the reader. The screen also stays on while reading, and goes back to the phone's normal sleep timeout after **5 minutes without a page turn**, the same idle limit as reading sessions, so a book left open doesn't keep the screen on all night. Approved with the Phase 9 plan. |
| D32 | Where "Continue reading" goes for series in the grid (#52) | **A button on Series Details only.** A floating "Continue reading" button opens the volume the series card's button would open. It isn't shown when every volume is finished. The grid keeps 3 covers per row and gets no button of its own. Approved with the Phase 9 plan. |
| D33 | How bookmarks appear in the Highlights tab (#51) | The tab keeps the name **Highlights**, and the screen gets two tabs you can swipe between: **Highlights** and **Bookmarks**. Bookmarks are grouped by series and book like highlights, show the chapter, page and start of the page's text, and open the reader at that page. Search covers both. Export stays for highlights only. Approved with the Phase 9 plan. |
| D34 | How see-through the reader's bars are (#53) | **Plain semi-transparent**, about 85% opaque so the bar's text stays readable over any page. The frosted-glass look isn't proposed: blurring what's behind the bars only works on Android 12 and newer, and it would need an extra library. Approved with the Phase 9 plan. |
| D35 | How illustrations open (#50) | **A long press on an illustration** opens it full-screen over the reader, on a black background, where you can pinch or double-tap to zoom and drag to move around. Back or a tap on the close button returns to the same page. A long press on text still selects text. Illustrations are also centred on the page. Approved with the Phase 9 plan. |
| D36 | How the reader's themes are picked, now that there are five (#55) | Five names don't fit in one row of buttons on a phone ("Dark grey" was cut to "Dark"), so the theme row became **a row of round swatches**, each showing "Aa" in the theme's colours with its name underneath. Auto's swatch is half light and half dark. Built this way in part 1, in the reader's sheet and in Default reader settings, and approved with it. |

All the details are in [local_storage_and_library.md](local_storage_and_library.md).

### Open
None right now.

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
Work from the GitHub issues filed on 2026-11-17. This phase is split into six parts, smallest first. Each is on its own feature branch and merged into `1.1.0` (D23). The open decisions D24 to D28 apply.
1. **Reader fixes and layout (S–M):**
   - [#31](https://github.com/naqibrafiqi/pustaka/issues/31) (bug): the Contents sheet shakes when you swipe up on a list too short to scroll. A short list should stay still.
   - [#32](https://github.com/naqibrafiqi/pustaka/issues/32): justified paragraphs (D24). Readium only justifies when the book's own styling is off, so the switch has to work with the "Original" line-spacing option.
   - [#33](https://github.com/naqibrafiqi/pustaka/issues/33): space at the bottom of the page. Readium's margin setting only covers the left and right.
   - [#34](https://github.com/naqibrafiqi/pustaka/issues/34): centred chapter titles. Each book's own styling decides this, so Pustaka adds a small style rule of its own.
2. **Series cover from the latest volume (S + M):** [#38](https://github.com/naqibrafiqi/pustaka/issues/38). The series cover comes from the last volume instead of the first (D25), and Edit series lets you pick any volume's cover.
   - The series card, the compact grid, search and the front of the Series Details header all show the series cover. The card used to show the cover of the volume being read, so a picked cover now shows there too.
   - A picked cover goes back to the default when its volume is removed, hidden or its file changes.
3. **Back to where you were (M):** [#35](https://github.com/naqibrafiqi/pustaka/issues/35). After a jump, the reader remembers the previous page and shows a button to go back to it (D26).
   - Built as proposed in D26: a "Back to page 42" button after a jump from the contents, the page slider or a highlight in the Highlights tab. It shows and hides with the top and bottom bars, just above the page slider. Jumping again keeps the first page, the one that was being read.
   - "10 pages" means 10 screen turns (swipes or taps) forward from where the jump landed, as you asked in review. A turn back takes one off the count. When scrolling, there are no turns, so the page numbers scrolled are counted instead. Links inside the book, such as footnotes, aren't counted as jumps.
4. **Bookmarks (M):** [#37](https://github.com/naqibrafiqi/pustaka/issues/37). A bookmark button in the reader's top bar, a new database table, and a list of bookmarks (D27).
   - Built as proposed in D27: a bookmark button in the top bar, next to Contents, is filled when the page is bookmarked. Tapping it bookmarks the page, or removes the page's bookmark. The Contents sheet has two tabs, Chapters and Bookmarks. Bookmarks are listed in book order with the chapter, page, percentage and the start of the page's text. Tapping one goes there, and "Back to page 42" (D26) then offers the page that was left. Each one has a button to remove it.
   - A bookmark belongs to a page number. A page number can take more than one screen, so every screen of a bookmarked page shows the button filled.
   - Bookmarks are kept in a new `bookmarks` table, so the database goes from version 1 to 2. Room adds the table when 1.1.0 is installed over 1.0.0, and everything else is kept. A test opens a 1.0.0 database to check this.
5. **Tablet layouts (M):** [#39](https://github.com/naqibrafiqi/pustaka/issues/39). Check every screen on a tablet, upright and sideways, and fix anything that overlaps, overflows or has text that's too small.
   - Checked every screen on a tablet-sized emulator (800 × 1280dp), upright and sideways, and at the phone size afterwards. Nothing overlapped or ran off the screen, but most screens were laid out for a phone's width, so on a tablet the covers grew very large and lines stretched right across. Fixed:
   - **Grids and lists fit more across.** The Library grid shows 4 covers upright and 6 sideways (2 on a phone, as before). The Library list, the series cards and the Series Details volumes go two or three across. The compact series grid fits 5 or more. A phone keeps its current layout.
   - **Covers stop growing.** The list layout's cover stays the size it is on a phone, and the cover on Book Details stops at 240dp wide, so the title and the read button show without scrolling, even sideways.
   - **Readable width.** Settings, Highlights, Search, Book Details, Series Details, Edit, Hidden items, About, Licences, Default reader settings and the access screens keep their content to a column in the middle (720dp, or 840dp for Series Details) instead of stretching across the screen.
   - **Navigation rail** on wide windows instead of the bottom bar (D29).
   - **Screen edges.** Every screen left the space for the system bars and camera cutout twice, so sideways the content was pushed in on one side and lists ended with an extra gap at the bottom. This also affected phones, on the screens opened from a tab.
   - **Text size:** all text uses Android's scalable text sizes, so it's the same physical size on a tablet as on a phone and follows the system font size setting. Nothing needed changing. The reader shows two pages side by side when the tablet is on its side, which Readium does on its own.
6. **Page-flip animation (L):** [#36](https://github.com/naqibrafiqi/pustaka/issues/36). A third page-turn choice in reader settings, next to Paged and Scroll. **Set aside for a later version (D28).** The feasibility check found a curl could be built on top of Readium, but the result was far from what you have in mind, so it isn't in 1.1.0.

**Done when:** every issue above is fixed on `1.1.0`, or deliberately set aside with your agreement.

**Wrapping up 1.1.0:** on branch `release-1.1.0`, from `1.1.0`:
- Version 1.1.0 (version code 2) and the [release notes](releases/1.1.0.md).
- A simpler app icon, as you asked: a flat cream open book with the green ribbon on a plain brown background, without the cover, lines of text, shadow and gradient. The colours are still D19's.
- Screenshots in the README, of public-domain books from [Standard Ebooks](https://standardebooks.org), so no copyrighted covers or text are shown.
- Two fixes found while taking them. An author also listed with another role, such as annotator, was shown as "Unknown author". A long genre on a series card squeezed the volume count into a thin bar. Books already in the library pick up the author fix when their file changes.

### Phase 9: Improvements after 1.1 (L)
Work from the GitHub issues filed on 2026-11-18, [#50](https://github.com/naqibrafiqi/pustaka/issues/50) to [#56](https://github.com/naqibrafiqi/pustaka/issues/56). #36 (page flip) stays set aside (D28). This phase is split into three parts, smallest first. Each is on its own feature branch, started from `1.2.0` and merged into it. Decisions D30 to D35 apply.

**README and screenshots:** each part updates [README.md](../README.md) before it's reviewed: the Features list for anything new you can see or use, and the screenshots it changes, retaken the same way as for 1.1.0 (the Standard Ebooks books on the emulator, a clean status bar). The screenshots each part changes are listed with it.

1. **Reader and tablet polish (S–M):**
   - [#56](https://github.com/naqibrafiqi/pustaka/issues/56): the tabs in the tablet's navigation rail are centred down the side instead of starting at the top.
   - [#53](https://github.com/naqibrafiqi/pustaka/issues/53): the reader's top and bottom bars are semi-transparent (D34), so the page shows through. The "Back to page 42" button keeps its own colour.
   - [#54](https://github.com/naqibrafiqi/pustaka/issues/54): the reader keeps its own brightness, with a slider in reader settings, and keeps the screen on while reading (D31).
   - [#55](https://github.com/naqibrafiqi/pustaka/issues/55): a **Dark grey** theme in reader settings and Default reader settings (D30). Readium has no such theme, so it's Readium's Dark theme with Pustaka's own background and text colours. The reader's bars and sheets match it.
   - Screenshots: `reader.png` (see-through bars), `tablet.png` (centred rail), and a new `reader-dark-grey.png` in the empty spot in the README's second row.
   - Built as planned, on branch `phase-9a-reader-tablet-polish`:
     - **Rail:** the four tabs sit in the middle of the side, with equal space above and below.
     - **Bars:** 85% opaque, in the same colour as before. Their shadow is gone, since it showed through. The bars are dark grey with the Dark grey theme.
     - **Brightness:** the slider is the first setting. It follows the same curve as the phone's own slider, so the middle looks the same in both. While it's dragged, the screen shows the brightness under your thumb, in the reader and in Default reader settings. **Reset** puts back how the page looks and leaves the brightness where you set it.
     - **Dark grey:** light grey text (#DCDCDC) on a dark grey page (#303030). The bars and sheets are dark grey with it, even when the app is light. The other themes keep their bars as before.
     - **Themes are picked from swatches (D36),** because five names didn't fit in a row of buttons.
   - Approved and merged into `1.2.0`, with D36.
2. **Series and bookmarks (M):**
   - [#52](https://github.com/naqibrafiqi/pustaka/issues/52): a "Continue reading" button on Series Details (D32).
   - [#51](https://github.com/naqibrafiqi/pustaka/issues/51): bookmarks in the Highlights tab, next to highlights (D33). Bookmarks are already saved in their own table, so the database doesn't change.
   - Screenshots: `series-details.png` (the new button) and `highlights.png` (the two tabs), plus a new `bookmarks.png` of the Bookmarks list.
   - Built as planned, on branch `phase-9b-series-bookmarks`:
     - **Continue reading:** a floating button at the bottom of Series Details. It opens the same volume as the series card's button: the volume being read that was opened last, or else the first one that isn't finished. It isn't shown once every volume is finished. The list leaves room at the end so the button doesn't cover the last volume.
     - **Two tabs:** Highlights and Bookmarks, with their counts in the tab names, like the Bookmarks tab in the reader's Contents sheet ("Bookmarks (3)"). The count under the title is gone, since the tabs show it. You can swipe between the tabs or tap them, and each has its own message when it's empty.
     - **Bookmarks:** grouped by series and book like highlights, in book order. Each one shows the chapter, the start of the page's text, and the page and how far into the book it is. Tapping one opens the reader at that page, with "Back to page" to return to where you were. The chapter comes from what the reader saved with the bookmark, so the book doesn't have to be opened to list it.
     - **Removing a bookmark:** each bookmark has the same remove button as in the reader's Contents sheet. This wasn't in D33, so it can be taken out if you'd rather keep the list read-only.
     - **Search and export:** the search looks at both lists (bookmarks by chapter, text, book and series). Export stays for highlights only, and its menu only appears when there are highlights.
     - **Screen readers:** Material 3 hides the text of a floating button with a label from screen readers, so the new button, and the Series tab's "New series" button, which had the same problem, now name themselves on their icon.
     - **README:** `bookmarks.png` starts a third row of screenshots, which part 3's `illustration.png` can join.
   - Approved and merged into `1.2.0`, with the remove button on bookmarks.
3. **Illustrations (L):** [#50](https://github.com/naqibrafiqi/pustaka/issues/50). A long press on an illustration opens it full-screen with zoom (D35), and illustrations are centred on the page.
   - Readium doesn't report presses on images, so the script Pustaka already adds to each chapter (the one that justifies paragraphs and centres chapter titles) also watches for a long press on an image and tells the app which one. The app loads the image from the book itself, at full size.
   - Centring is done by the same script, since adding a stylesheet to a chapter turns off Readium's own default styles.
   - Text selection has to keep working as before. A long press on text next to an illustration should select the text, not open the image.
   - Screenshots: a new `illustration.png` of a zoomed-in illustration, from a public-domain book that has illustrations, in a new row in the README.

**Done when:** every issue above is fixed on `1.2.0`, or deliberately set aside with your agreement, and the README describes and shows what's in 1.2.0.

**Wrapping up 1.2.0:** on branch `release-1.2.0`, from `1.2.0`: version 1.2.0 (version code 3), the release notes in `releases/1.2.0.md`, the README's status line pointing to them, and a last check that every screenshot matches the app.

---

## Working agreement

- **Branches:** one feature branch per phase, or per sub-feature for the large phases. Each starts from the version branch it's for (`1.1.0` for Phase 8, D23, and `1.2.0` for Phase 9). Claude creates each version branch locally from the one before it. Released versions such as `1.0.0` aren't changed. Version branches are merged into `main` separately.
- **Local only:** Claude works only in the local repository. It never pushes to GitHub, opens pull requests or changes anything there. When you approve a feature branch, Claude merges it into the local version branch. You push branches and handle pull requests, issues and releases yourself.
- **Commit messages:** follow the existing style, e.g. `feat: …` and `fix: …`.
- **README:** every feature branch that changes what you can see or do in the app updates README.md, including its screenshots, before it's reviewed.
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
| 8. Fixes and improvements after 1.0 | Done, for 1.1.0. Parts 1 (#31 to #34), 2 (#38), 3 (#35), 4 (#37) and 5 (#39) are merged. Part 6 (#36, page flip) is set aside for a later version (D28). The 1.1.0 wrap-up (version, icon, screenshots, release notes) is merged. 1.1.0 was published on 2026-11-17. |
| 9. Improvements after 1.1 | In progress, for 1.2.0. The plan and D30 to D35 are approved. Part 1 (#53 to #56) is merged, with D36. Part 2 (#51, #52) is merged. Next: part 3 (#50). |
