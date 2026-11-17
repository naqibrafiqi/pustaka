# Local storage and library

_Last updated: 2026-11-14_

Pustaka builds its library from a folder that the user manages. The user adds, removes and organises EPUB files with any file manager or sync tool, and the app reads that folder and shows what it finds. **The app never renames, moves or edits the user's files.** The only time it changes the folder is when the user explicitly asks to delete something (see [Deleting](#deleting)).

---

## Folder layout

The default library folder is `/storage/emulated/0/Pustaka`. The user can change it in Settings, for example to `/storage/emulated/0/Download/Pustaka`.

```
Pustaka/
├── The Left Hand of Darkness.epub        → standalone book
├── Playing Death Games/                  → series
│   ├── Playing Death Games, Vol. 01.epub → volume
│   ├── Playing Death Games, Vol. 02.epub → volume
│   └── extras/                           → ignored (only one level is scanned)
├── My New Series/                        → series with 0 volumes
└── .hidden/                              → ignored (name starts with ".")
```

| What's in the folder | What the app shows |
|---|---|
| `.epub` file directly inside `Pustaka/` | A **standalone book**: shown in Library but not in the Series tab |
| A folder directly inside `Pustaka/` | A **series**, even if it's empty |
| An `.epub` file inside a series folder | A **volume** of that series |
| Folders inside a series folder | Ignored |
| Any file that isn't an `.epub` | Ignored |
| Files and folders whose names start with `.` | Ignored |

---

## Storage access

The app is distributed through GitHub (sideloaded), not the Play Store, so it uses Android's **"All files access"** permission to read the folder directly.

| Android version | Permission |
|---|---|
| 11 and newer (API 30+) | `MANAGE_EXTERNAL_STORAGE`. The user turns it on in a system settings screen that the app opens for them |
| 10 (API 29) | `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`, with `requestLegacyExternalStorage="true"` in the manifest |

**First launch:**
1. A welcome screen explains why the app needs access to files, then opens the permission screen.
2. Once access is granted, the app creates `Pustaka/` if it doesn't exist and runs the first scan.
3. If access is refused, the app shows a screen with a "Grant access" button instead of the library.

Access is checked again every time the app comes back to the foreground, since the user can turn it off in the system settings at any time. Without access, the app shows the "Grant access" screen again.

Android doesn't let an app run anything when it's installed, so the folder is created on first launch, after access is granted.

---

## Scanning

**When it runs:**
- every time the app is opened or brought back to the foreground
- when the user pulls down to refresh in Library or Series
- when the user taps "Rescan library" in Settings

**How it works:**
1. List the files and folders in `Pustaka/`, then list the files in each series folder.
2. Compare each file's path, size and last-modified time with what the database already has. Only files that are **new or changed** get opened and read, so a refresh with nothing new is quick.
3. Match files that have been moved or renamed to books already in the library (see [Book identity](#book-identity)).
4. Mark books whose files have disappeared as **missing** (see [Missing files](#missing-files)).
5. Show a short result, for example "3 new books found" or "Library is up to date".

Scans run in a background job (WorkManager), so they keep going if the user leaves the app. Only one scan runs at a time: asking for a scan while one is waiting or running joins that one.

The result is shown as a short message. Scans the user asks for (pull-to-refresh or "Rescan library") always show one, even if it's only "Library is up to date". Automatic scans (when the app opens or comes back) only show one when something changed, so opening the app stays quiet.

Each scan also creates the library folder if it's missing, as long as the app has access to the files.

---

## Book identity

Because the user controls the filenames, the app has to recognise a book after it's been renamed or moved. Otherwise reading progress and highlights would be lost. Each book is identified by a **fingerprint**: a hash of the file's size plus the first and last 64 KB of its contents.

The ID stored inside the EPUB (`dc:identifier` in `content.opf`) is saved too, but it isn't used on its own. Publishers sometimes reuse the same ID for every volume in a series.

**On each scan:**
- A new path with a **known** fingerprint is treated as a move or rename. The app updates the book's path and keeps its progress, highlights and edited details.
- A new path with an **unknown** fingerprint is a new book.
- An exact copy of a book that's still in its old place is a second book, because both files exist.
- A file that keeps its path but changes (its size or last-modified time is different) is read again. It keeps its progress and edited details.

**Series folders** are matched by their path. When a series folder is renamed, its books turn up under a new path with known fingerprints, so the app treats the folder as the same series. It keeps the series' edited name and details, and whether it's hidden. A series whose folder has gone is removed once it has no books left. While it still has missing books, it stays.

---

## Metadata

The EPUB files are read with the **[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit)** (BSD-3-Clause licence), using its `readium-shared` and `readium-streamer` modules. Phase 4 uses the same library to display books, so one library handles both jobs. Readium reads each book's `content.opf` file (EPUB 2 and 3) and provides:

| Field | Where it comes from in `content.opf` | Where Pustaka uses it |
|---|---|---|
| Title | `dc:title` | Book details ("Title in file") |
| Authors | `dc:creator` with role `aut`, or no role | Book cards, details |
| Illustrators / translators | `dc:creator` / `dc:contributor` with role `ill` or `trl` | "Author • Illustrator" lines |
| Publisher | `dc:publisher` | More info |
| Publication date | `dc:date` | More info |
| Identifier | `dc:identifier` (the `unique-identifier` ID) | More info, and a second check for identity |
| Language | `dc:language` | More info |
| Description | `dc:description` | Synopsis |
| Subjects | `dc:subject` | Genres and tags |
| Series and volume number | `belongs-to-collection` + `group-position` (EPUB 3), or `calibre:series` + `calibre:series_index` | Volume order |
| Cover | the manifest item marked as the cover | Cover images, saved in the app's cache |
| Total pages | Readium's position list | "Page X of Y" |

Readium counts every `dc:creator` as an author, whatever its role, so an illustrator listed as `<dc:creator opf:role="ill">` would show up as an author. Pustaka reads the creators and their roles from `content.opf` itself: the `opf:role` attribute in EPUB 2, and `<meta property="role" refines="…">` in EPUB 3. A `dc:contributor` with no role is saved with the role `ctb` ("contributor"), so it isn't taken for an author.

A file that can't be opened, for example because it's damaged, is still added to the library with its filename as its name. It's read again when the file changes.

### Default names and editing
- **Series name** is the folder name by default.
- **Book name** is the filename without `.epub` by default.
- The user can edit every series and book detail, names included. Edits are saved in the app's database and **never written into the files**. Each field can be reset to its default.
- On the "Edit details" screen, a "Use details from file" button fills the fields from the EPUB's own details. This helps when a filename is untidy, such as `[Group] Series v04 (Digital).epub`.
- Series details other than the name are filled from the series' first volume by default, using the details in that volume's file: its authors and illustrators, its description as the synopsis, its first subject as the main genre and the other subjects as tags.
- The series cover is the **last volume's** cover by default (D25), so it changes when a new volume is added. Edit series can pick any volume's cover instead. A picked cover goes back to the default if that volume is removed, hidden or its file changes.

---

## Volume order

The order of volumes within a series is decided by the first rule that gives an answer:
1. **The user's own number**, if they've set one in Edit details.
2. **The volume number stored in the EPUB** (`group-position` or `calibre:series_index`).
3. **A number in the filename**, such as `Vol. 04`, `Vol 4`, `v04`, `#4`, `Volume 4`, or a number at the end of the name.
4. **The filename itself**, sorted so that `10` comes after `9`.

Books with the same number are ordered by filename.

---

## Missing files

When a known file is no longer found, its book is marked **missing** instead of being deleted:
- It still appears, greyed out with a "File not found" label, and it can't be opened.
- Its progress, highlights and edited details are kept.
- If a file with the same fingerprint turns up again, anywhere in the library, it's restored automatically.
- The user can remove a missing book from the library for good. Its data is deleted.

---

## Deleting

The app only changes the folder when the user asks it to.

| Action | Choices offered | Effect |
|---|---|---|
| Delete series | **Remove from library** | Hides the series and its books. They don't come back on the next scan, and can be restored in Settings → Hidden items. The folder isn't touched |
| | **Delete folder and files** | Deletes the folder and every EPUB in it after a confirmation that shows how many files will go. Can't be undone |
| Delete an empty series | (one choice) | Deletes the empty folder |
| Delete book | **Remove from library** / **Delete file** | Same as for a series, for one file |

---

## Out of scope for v1
- Moving or renaming files from inside the app. Users do this with a file manager, and the app picks up the change through the fingerprint.
- Folders nested deeper than one level inside `Pustaka/`.
- Formats other than EPUB (PDF, CBZ).
- Watching the folder for changes while the app is closed.
- Storing edited details next to the books (for example a `.pustaka.json` file) so they survive reinstalling the app. This is a possible later addition.
