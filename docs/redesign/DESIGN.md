# Pustaka 2.0 design: "Ink & Stamp"

The mockups are on the design canvas: https://claude.ai/artifact/W3JTP4BMnjhp2zYb9mPZCF (private until you share it). Every phone board has a **dark** switch in its Tweaks panel. The boards marked as interactive can be clicked through in Play mode.

This document is the spec the mockups illustrate. [PLAN.md](PLAN.md) splits it into phases you build yourself. Each part is a proposal: you decide whether to adopt, change or keep the current design, phase by phase.

## 1. The idea

Pustaka means *library*. The 1.x design borrowed Google Play Books' language: warm paper, amber-brown and generic cards. 2.0 gives Pustaka its own look by taking the objects of a real library and making them the interface:

| Library object | In the app |
|---|---|
| A bookmark ribbon | Reading progress on a cover |
| Spines on a shelf | Recently added books, and a Spines view of series |
| A catalogue card | The facts about a book on Book details |
| A date stamp | Days you read, READ and FINISHED |
| An index card | Highlights, with your note on the back |
| A library ticket | The streak card and the library summary |

The colours come from the same desk: **ink blue** for actions, **stamp red** for marks and dates, and **ribbon yellow** for progress and "you are here", all on off-white **paper**. It's still Material 3, using M3 components and colour roles with Pustaka's own values. Fun comes from motion with a physical reason, never from decoration while you read.

## 2. Foundations

### 2.1 Colour

Mapped to the M3 `ColorScheme` roles, so M3 components pick them up without extra work.

| Role | Light | Dark | Used for |
|---|---|---|---|
| primary | `#2B3F8C` | `#AEBBFF` | Ink: main buttons, links, progress fill |
| onPrimary | `#FFFFFF` | `#14205A` | |
| primaryContainer | `#DDE3FF` | `#2B3A78` | Now-reading bar, selected states |
| onPrimaryContainer | `#1A2A66` | `#DDE3FF` | |
| secondary | `#7A5900` | `#F5C25A` | Ribbon as text or icons (rare) |
| onSecondary | `#FFFFFF` | `#3F2E00` | |
| secondaryContainer | `#F2B33D` | `#F5C25A` | Ribbon: nav indicator, FAB, current volume |
| onSecondaryContainer | `#2A1E00` | `#2A1E00` | |
| tertiary | `#C63D22` | `#FF8F70` | Stamp: stamps, today's mark, the card's red rule |
| onTertiary | `#FFFFFF` | `#5A1404` | |
| tertiaryContainer | `#FFDCD3` | `#7A2A18` | |
| onTertiaryContainer | `#5A1404` | `#FFDCD3` | |
| surface / background | `#F6F2EA` | `#12151C` | Paper |
| onSurface | `#1B1F2A` | `#ECE8DF` | Type |
| onSurfaceVariant | `#5A5F6E` | `#A3A7B3` | Secondary text |
| surfaceContainerLowest | `#FFFFFF` | `#0D1016` | |
| surfaceContainerLow | `#FFFFFF` | `#1B1F29` | Cards |
| surfaceContainer | `#ECE5D8` | `#232836` | Tiles, chips, the "shelf" grey |
| surfaceContainerHigh | `#E6DFD1` | `#2C3242` | |
| surfaceContainerHighest | `#E0D8C9` | `#343A4A` | |
| outline | `#7A7F8C` | `#8C91A0` | |
| outlineVariant | `#D9D0C0` | `#343A4A` | Borders, dividers |
| inverseSurface | `#1B1F2A` | `#ECE8DF` | Snackbars |
| inverseOnSurface | `#ECE8DF` | `#1B1F2A` | |
| inversePrimary | `#AEBBFF` | `#2B3F8C` | |

**Extended colours.** These are outside M3's roles. Provide them through your own `PustakaColors` and a `staticCompositionLocalOf`:

| Token | Light | Dark | Used for |
|---|---|---|---|
| navContainer | `#1B1F2A` | `#2C3242` | The floating nav pill |
| onNavContainer | `#ECE8DF` | `#ECE8DF` | |
| plank | `#8A6A45` | `#5C4A36` | The shelf board under the spines |
| cardRule | `#C9D2F0` | `#33406B` | Ruled lines on the catalogue card |
| indexRule | `#E05A3A` | `#E05A3A` | The red top line on index cards |
| highlightYellow / Green / Blue / Pink (paper) | `#FFF4C7` `#DFF1D6` `#DCE8FF` `#FFE0E6` | same | Index cards (they stay light in dark mode, like paper) |

**Book tint.** Each book gets a colour taken from its cover. Spines and the Continue card use it at full strength. Tinted containers mix it at about 15% over `surfaceContainerLow`. The plan covers extracting and caching it.

Every text pairing must reach 4.5:1. The project already has `ColorSchemeContrastTest`, so extend it to the new values and the extended colours.

### 2.2 Type

Bundle the fonts in `res/font`, because the app is offline and can't rely on downloadable fonts. All four are under the SIL Open Font License, so add them to the open-source licences screen.

| Family | Role |
|---|---|
| **Bricolage Grotesque** (variable, 400–800) | display, headline, and the titles of books and series |
| **Plus Jakarta Sans** (400–800) | title, body, label: everything else in the UI |
| **DM Mono** (400, 500) | Catalogue fields, stamps, page numbers, small caps labels (`VOL. 02 · P. 128`) |
| **Literata** (italic) | Quotes on index cards and quote cards. The reader keeps its own fonts. |

| M3 style | Family | Size / line | Weight | Tracking |
|---|---|---|---|---|
| displaySmall | Bricolage | 34 / 36 | 800 | -0.8 |
| headlineMedium | Bricolage | 30 / 32 | 800 | -0.6 |
| headlineSmall | Bricolage | 22 / 26 | 700 | 0 |
| titleLarge | Bricolage | 20 / 24 | 700 | 0 |
| titleMedium | Jakarta | 16 / 22 | 700 | 0 |
| titleSmall | Jakarta | 14 / 20 | 700 | 0 |
| bodyLarge | Jakarta | 16 / 24 | 400 | 0 |
| bodyMedium | Jakarta | 14 / 21 | 400 | 0 |
| bodySmall | Jakarta | 12 / 16 | 400 | 0 |
| labelLarge | Jakarta | 14 / 20 | 700 | 0 |
| labelMedium | Jakarta | 13 / 18 | 600 | 0 |
| labelSmall | DM Mono | 11 / 14 | 500 | +1.2, upper case |

Add one extra style, `PustakaTypography.stamp` (DM Mono 500, upper case, +1.5 tracking), for stamps.

### 2.3 Shape, spacing and elevation

- Corner sizes: extraSmall 4, small 8, medium 16, large 22, extraLarge 28, plus a pill. Covers use 6–8.
- Grouped list items (Settings) are 20 on the group's outer corners and 6 inside, with 3dp gaps.
- **Ticket shape:** a rounded rectangle with a semicircle notch in the middle of the bottom edge. Make it a custom `Shape` that builds a `Path`, so it clips and outlines properly.
- Spacing is on a 4dp grid: 4, 8, 12, 16, 20, 24, 32. Screens have 16–20dp side margins.
- Mostly flat. Real shadows are only for physical objects: covers, index cards and the nav pill.
- Touch targets are 48dp minimum.

### 2.4 Motion

| Token | Spec | For |
|---|---|---|
| `spatial` | `spring(dampingRatio = 0.8f, stiffness = 380f)` | Things moving or resizing |
| `spatialBouncy` | `spring(0.6f, 500f)` | Spine pull-out, nav indicator |
| `stamp` | `spring(0.45f, 600f)` on scale 1.6 → 1, rotation ±8° → final | Stamps landing |
| `effects` | `tween(200, easing = FastOutSlowInEasing)` | Fades, colour changes |
| `stagger` | 40ms per item, at most 8 items | Cards, stamps, heatmap weeks |

Rules:
1. **Objects behave like objects.** Spines slide, cards flip, stamps land, ribbons fill.
2. **Covers keep their place.** A cover is a shared element between list, details and reader.
3. **Quiet in the reader.** Nothing moves while you read.
4. **Reduce motion.** If the system's animator duration scale is 0, replace movement with fades, or with nothing.

## 3. Components

| Component | What it is | Notes |
|---|---|---|
| `BookCover` (update) | A cover image with an optional **ribbon** showing % | The ribbon is a custom `Shape` (a rectangle with a V notch), placed top-right |
| `Spine` | A coloured vertical bar with the title rotated 90° | Width from the book's file size or page count, height from a hash of the title (stable) |
| `Shelf` | A custom `Layout` that bottom-aligns spines on a plank | Scrolls sideways when full |
| `TicketCard` | A card in the ticket shape | Streak, library summary |
| `Stamp` | Bordered mono text, rotated | Has a `landed` animation state |
| `SegmentedProgress` | One segment per volume: read, current or unread | Series card, Series details |
| `VolumeStepper` | Numbered circles joined by a line | Series details |
| `NavPill` | A floating bottom nav with a sliding ribbon indicator | A custom composable, not `NavigationBar` |
| `NowReadingBar` | A mini bar for the last book: cover, title, page, ring and play | Above the nav pill |
| `IndexCard` | A paper card with a red rule, front and back | Notes |
| `CatalogueCard` | Ruled fields, a DATE READ column of stamps, a punch hole | Book details |
| `GroupedList` | M3-expressive grouped rows | Settings |

## 4. Screens

### 4.1 Navigation (shell)

- **3 tabs instead of 4: Home, Shelves, Notes.** Settings moves to an icon on Home's top bar, as a pushed screen in the Home back stack.
- **The nav pill** floats 12dp above the bottom inset, in `navContainer`. The selected item shows its icon and label on a ribbon-coloured pill; the others show only an icon. Switching tabs slides and stretches the indicator.
- **Now-reading bar** sits above the pill on the three tab roots. Tapping it opens Book details; tapping play opens the reader. It hides when nothing is in progress.
- On a tablet the pill becomes a vertical rail pill on the left (D29 stays: the rail is on every screen).

*Keep option:* keep the 4-tab M3 `NavigationBar` but restyle it with the new tokens.

### 4.2 Home (was Library)

1. A greeting with the date in mono ("WEDNESDAY · 25 NOV"), "Good morning / afternoon / evening", and Search and Settings icons.
2. **Streak ticket:** the current streak in days, minutes in the last 7 days, and Monday–Sunday dots (ink = read, stamp ring = today). Tap it to open the Reading journal.
3. **Continue reading carousel:** every book in progress, most recent first. The card is tinted with the book's colour and shows the cover with its ribbon, title, author, "CH. 4 · 9 PAGES LEFT", progress, and Resume. Neighbouring cards scale to 0.9 and tilt 1.5° by pager offset.
4. Filter chips: All, Reading, Unread, Finished, with counts. They filter the shelf and the grid below.
5. **Your shelf:** the books added most recently, as spines. Tapping a spine pulls it out; a row under the shelf shows its title, author and an Open button.
6. **All books** in a 3-column grid with ribbons.

### 4.3 Shelves (was Series)

- A Cards | Spines segmented toggle (the Spines view reuses `Shelf`), and a sort menu.
- **Series card:** a tinted container holding a fan of three covers, "5 VOLUMES", the title and author, `SegmentedProgress`, "Up next · Vol. 2, The Sign of the Four", a Continue button and a volumes button.
- A "Standalone books" row at the bottom goes to Home filtered to standalone books.
- The New series action stays as a ribbon-yellow FAB.

### 4.4 Series details

- **Hero:** a deep version of the series colour, with the fanned covers (a shared element from the card), title, author, and "5 VOLUMES · 3.3 MB · ALL INDEXED".
- A synopsis with More.
- A "Your progress" card with `VolumeStepper`.
- Volumes list: VOL. 01 in mono, the title, and a status that is a READ · 18 NOV stamp, a progress bar, or "Not started". The current volume is tinted ribbon.
- The folder path in a quiet chip.
- An extended FAB, "Continue Vol. 2", that shrinks to an icon when you scroll.

### 4.5 Book details

- The cover with its ribbon on a tinted arch, as a shared element. Above the title, "SHERLOCK HOLMES · VOL. 2" links to the series.
- "Continue from page 14", plus a button for bookmarks and contents.
- Three stat tiles: pages/hour, time left at your pace, and sessions (from `ReadingStats`).
- **Catalogue card:** AUTHOR, TITLE, SERIES, PAGES, ADDED, FILE and SIZE as ruled mono fields, a DATE READ column with one stamp per day you read (the last four), and a punch hole. The stamps land one by one when the card scrolls into view.
- Subjects as chips, then the synopsis.
- "Your notes": the latest highlight as an index card, with "See all".

### 4.6 Notes (was Highlights)

- A Highlights · 6 | Bookmarks · 3 segmented button, and a chip row to filter by book.
- Highlights are **index cards** in a 2-column staggered grid. Each is coloured by highlight colour, slightly rotated, and shows "SCARLET · P. 13" and the quote in Literata italic. A folded corner means there's a note: tap to flip the card and read it, with Edit and Share card.
- Bookmarks stay a list, restyled with the mono page and chapter.
- Export to Markdown stays, in the top bar.

### 4.7 Quote card (new)

- Opened from a highlight's Share card, or from the reader's selection toolbar.
- A 4:5 image: highlight-colour paper, a book-colour edge, a big stamp-red quote mark, the quote, the title, "AUTHOR · P. 13", and a small PUSTAKA stamp.
- Save image and Share.

### 4.8 Reading journal (new)

- Opened from the streak ticket.
- A year picker, then four totals: time read this year, books finished, streak (with the best), and average pages/hour. Their numbers count up.
- **Days you read:** a 16-week heatmap in five ink levels, today ringed in stamp red, future days dashed.
- **Last 7 days:** a bar chart of minutes, today in ribbon.
- **Finished in 2026:** covers with FINISHED date stamps.

### 4.9 Reader (small upgrades only)

You like the reader, so it keeps its structure. The changes:

1. A **chapter chip** in the bottom bar ("6 pages left in chapter") and a **session timer** ("12 MIN").
2. **Chapter ticks** on the page slider. The thumb snaps to a tick when you drag near it, with a light haptic.
3. **Selection toolbar** restyled as a dark pill: four colour dots (the current one ringed), then Note, Copy, and Share as quote card.
4. **Finished a volume:** a sheet with a FINISHED stamp that lands with a haptic, the totals for the volume, and the next volume to start. It replaces the current finish card.

### 4.10 Settings

- A **library ticket** at the top: "11 books · 3 series", size, last scan, and Rescan.
- Grouped lists: Library, Appearance, Reading, Storage, About.
- **Theme picker** as three mini-previews (System, Light, Dark).
- **Accent** (new): Ink, Forest, Plum or Rust. It swaps the primary roles only. Dynamic colours stays as an override.

## 5. Accessibility

- Contrast is enforced by test (section 2.1).
- Every icon-only button has a content description. Spines read as "Moby-Dick, by Herman Melville, tap to pull out".
- Decorative rotation (cards, stamps) never goes over 8°, and text inside stays selectable or readable by TalkBack.
- The heatmap gets a text summary for TalkBack ("Read on 46 of the last 112 days").
- Layouts hold up at 200% font scale: titles wrap, and stamps can shrink.
- Reduce motion is respected (section 2.4).
