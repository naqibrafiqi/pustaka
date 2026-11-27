# Pustaka 2.0: the redesign plan

**You write the code. Claude designs, plans and reviews.** The design is in [DESIGN.md](DESIGN.md) and on the canvas: https://claude.ai/artifact/W3JTP4BMnjhp2zYb9mPZCF.

## How we work

1. **Before a phase:** read its brief below and the boards it names. Record your call in the decisions table: *Adopt* (build it as designed), *Adapt* (say what changes) or *Keep* (leave the 1.x design; the phase shrinks to the token restyle and any new features). Ask me about anything in the brief.
2. **During the phase:** build on a branch named `redesign/rN-<name>`, cut from `2.0.0`. Everything stays local, as before; you handle the remote. When you're stuck, ask for a hint at the level you want:
   - **Nudge:** which concept or doc to look at.
   - **Direction:** the approach, in words.
   - **Sketch:** pseudocode or a small snippet, never the finished code.
3. **At the deadline** (Sunday night), say "review R*N*". I will:
   - read the diff against `2.0.0`
   - build `assembleDebug` and run the unit tests (and the UI tests when an emulator is running)
   - open the app on the emulator and compare it with the boards, in light and dark, at 200% font size, with animations off, and with TalkBack spot checks
   - write findings in four groups: **Must fix**, **Should fix**, **Nits** and **Nice work**. I point at problems; I don't fix them.
4. **After the review:** fix the Must-fix items. When I confirm, you commit, merge into `2.0.0`, and the next phase starts on Monday.

**Deadlines.** Each phase is due Sunday at 23:59 your time. **Must** items are what a phase needs to pass review; **Stretch** items are optional. If a phase is going to be late, say so before the deadline. It then moves into the next buffer week. Two phases never run at once.

**Docs aren't coding practice.** At review, I can update README.md and the screenshots for the screens a phase changed, if you want me to.

## Decisions

Fill in each one before its phase starts.

| # | Decision | Options | Your call |
|---|---|---|---|
| D37 | The Ink & Stamp identity (colours, fonts, shapes) | Adopt / Adapt / Keep warm paper | |
| D38 | 3 tabs plus a nav pill, with Settings moved to Home's top bar | Adopt / Keep 4 tabs (restyled) | |
| D39 | Now-reading bar | Adopt / Drop | |
| D40 | Home: streak ticket and Continue carousel | Adopt / Adapt / Keep | |
| D41 | Shelf of spines, and book colours from covers | Adopt / Adapt / Keep | |
| D42 | Shelves and Series details | Adopt / Adapt / Keep | |
| D43 | Book details as a catalogue card | Adopt / Adapt / Keep | |
| D44 | Notes as index cards, plus the quote card | Adopt / Adapt / Keep | |
| D45 | Reading journal (a new screen) | Adopt / Drop | |
| D46 | Reader upgrades and the finished sheet | Adopt each of 1–4 / Drop | |
| D47 | Settings and accent colours | Adopt / Adapt / Keep | |

## Schedule

Today is Wednesday 25 November 2026. Weekday evenings are short, and Saturday and Sunday are the main build days.

| Phase | What | Dates | Due |
|---|---|---|---|
| R0 | Foundations: tokens, fonts, theme | Thu 26 Nov – Sun 29 Nov | **Sun 29 Nov 2026** |
| R1 | Navigation shell: nav pill, now-reading bar | Mon 30 Nov – Sun 6 Dec | **Sun 6 Dec** |
| R2 | Home I: greeting, streak, Continue carousel | Mon 7 – Sun 13 Dec | **Sun 13 Dec** |
| R3 | Home II: book colours, the shelf, the grid | Mon 14 – Sun 20 Dec | **Sun 20 Dec** |
| — | Buffer and year-end break | Mon 21 – Sun 27 Dec | |
| R4 | Shelves, Series details, first shared elements | Mon 28 Dec – Sun 3 Jan | **Sun 3 Jan 2027** |
| R5 | Book details: the catalogue card | Mon 4 – Sun 10 Jan | **Sun 10 Jan** |
| R6 | Notes: index cards, flip, quote card | Mon 11 – Sun 17 Jan | **Sun 17 Jan** |
| — | Buffer | Mon 18 – Sun 24 Jan | |
| R7 | Reading journal | Mon 25 – Sun 31 Jan | **Sun 31 Jan** |
| R8 | Reader upgrades and the finished sheet | Mon 1 – Sun 7 Feb | **Sun 7 Feb** |
| R9 | Settings, accents, the accessibility and motion pass | Mon 8 – Sun 14 Feb | **Sun 14 Feb** |
| R10 | Release 2.0.0 | Mon 15 – Sun 21 Feb | **Sun 21 Feb 2027** |

Each phase brief below has these parts:
- **Goal**
- **Boards**
- **Must**
- **Stretch**
- **Done when**: what I check at review
- **You'll practise**: concepts, with docs to start from
- **Think about**: traps to reason through yourself; the answers aren't given

The Compose and Android skills in `.claude/skills` are good reading too: `compose`, `android-testing` and `kotlin-flows`.

---

## R0 · Foundations (due Sun 29 Nov)

**Goal:** the app wears Ink & Stamp everywhere, with no layout changes yet.

**Boards:** Identity: Ink & Stamp.

**Must**
- Create `redesign/r0-foundations` from the local `2.0.0`. `2.0.0` is `1.2.0` plus these redesign docs.
- Bundle the four font families in `res/font`. Build `Typography` from DESIGN.md section 2.2.
- Replace the light and dark color schemes with section 2.1. Add `PustakaColors` (the extended colors) through a `staticCompositionLocalOf`, with a `PustakaTheme.colors` accessor.
- `Shapes` from section 2.3, plus `TicketShape` as a custom `Shape`.
- A `PustakaMotion` object holding the motion tokens, and a `LocalReduceMotion` that reads the system's animator duration scale.
- Extend `ColorSchemeContrastTest` to cover the new pairings and the extended colors, and make it pass.
- Add the fonts to the open-source licenses screen.

**Stretch:** a `DesignCatalog.kt` of `@Preview`s showing every color, type style, shape and token, in light and dark.

**Done when**
- Every existing screen renders with the new colors and fonts.
- The tests are green.
- Dynamic colors still overrides the scheme when it's switched on.

**You'll practise:** M3 theming, custom design-system tokens, `CompositionLocal`, font resources, and the `Shape` and `Outline` APIs.
- https://developer.android.com/develop/ui/compose/designsystems/custom
- https://developer.android.com/develop/ui/compose/text/fonts
- https://developer.android.com/develop/ui/compose/graphics/draw/shapes

**Think about**
- `staticCompositionLocalOf` or `compositionLocalOf`: which suits colors that change only with the theme, and why?
- When dynamic color is on, where do the *extended* colors come from?
- `TicketShape` must work at any size. What does `createOutline` receive, and which unit is the notch radius in?

---

## R1 · Navigation shell (due Sun 6 Dec)

**Goal:** the new frame of the app. Three tabs, a floating nav pill with a sliding indicator, a now-reading bar, and Settings moved.

**Boards:** Home (the bottom of the board), Shelves, Notes. Motion note 1.

**Must**
- `NavPill`: your own composable, not M3's `NavigationBar`. The selected item expands to show its label. The ribbon-colored indicator slides and stretches between items using `PustakaMotion.spatialBouncy`.
- Three tab back stacks: Home, Shelves, Notes. Settings becomes a route pushed from an icon on Home's top bar.
- `NowReadingBar` above the pill on the tab roots, showing the most recently opened in-progress book. Tapping it opens Book details; tapping play opens the reader. It hides when nothing is in progress.
- The pill and bar hide on pushed screens, as the bottom bar does now.
- Tablet: the side rail is restyled to match, and D29 stays.
- Update `AppNavigationTest` (and any other test that taps "Settings" in the bar).

**Stretch:** the pill hides while you scroll down and comes back when you scroll up (`NestedScrollConnection`).

**Done when**
- Every tab keeps its back stack.
- System back from a non-Home root goes to Home.
- The indicator animates and has no jump on first draw.
- TalkBack reads "Shelves, tab, 2 of 3" or similar.

**You'll practise:** `Animatable` and `animate*AsState` with springs, measuring children (`onGloballyPositioned` against a custom `Layout`), `AnimatedVisibility`, and Navigation 3 back stacks.
- https://developer.android.com/develop/ui/compose/animation/value-based
- https://developer.android.com/develop/ui/compose/layouts/custom
- https://developer.android.com/guide/navigation/navigation-3

**Think about**
- How does the indicator know where to go before the selected item has been measured?
- `selectedTabIndex` is saved. A 1.2 user who left the app on Settings (index 3) upgrades: what happens?
- The now-reading bar needs data on every tab. Which ViewModel owns it, and what is its lifetime?

---

## R2 · Home I: greeting, streak, carousel (due Sun 13 Dec)

**Goal:** the top half of Home, and your first new feature, the reading streak.

**Boards:** Home. Motion note 3.

**Must**
- **Streak logic as a pure Kotlin function**, written test-first: given the sessions, `now` and a `ZoneId`, return the current streak, minutes in the last 7 days, and which days this week you read. Rules:
  - A day counts if its sessions add up to at least 1 minute.
  - A session counts toward the day it started.
  - A streak isn't broken until a whole day has passed without reading, so today's not having a session yet doesn't break it.
- A DAO query for sessions since a given instant, and a repository function returning a `Flow`.
- The greeting, with the date in mono and morning, afternoon or evening.
- The streak ticket in `TicketShape`. Tapping it goes to a placeholder Journal route (the real screen comes in R7).
- **Continue carousel:** a `HorizontalPager` of the books in progress, with neighboring pages scaled and tilted by their page offset. Each card has the cover with its **ribbon** (build `RibbonShape`), "CH. n · x PAGES LEFT" if the data allows (otherwise page X of Y), and Resume.

**Stretch:** Resume morphs from a pill to a squircle while pressed (`androidx.graphics:graphics-shapes`: `RoundedPolygon` and `Morph`, driven by `interactionSource.collectIsPressedAsState()`).

**Done when**
- The streak tests cover: no sessions; today only; yesterday only; a gap; a session that crosses midnight; a session under a minute; a change of time zone.
- The carousel works with 1 book and with 10.
- With reduce motion on, the transforms are off.

**You'll practise:** test-driven Kotlin, `java.time`, Flows from Room, `HorizontalPager` and `PagerState.getOffsetDistanceInPages`, and `graphicsLayer`.
- https://developer.android.com/develop/ui/compose/layouts/pager
- https://developer.android.com/develop/ui/compose/graphics/draw/modifiers

**Think about**
- Why does the streak function take `now` and `ZoneId` as parameters instead of calling `Instant.now()`?
- `graphicsLayer { }` (the lambda) or `graphicsLayer(scaleX = …)` (the parameters): which avoids recomposing on every scroll frame?

---

## R3 · Home II: book colors, the shelf, the grid (due Sun 20 Dec)

**Goal:** the signature element. Books stand on a shelf as spines in their own colours.

**Boards:** Home (Your shelf and All books). Motion note 4.

**Must**
- **A color for each book from its cover.** Use AndroidX Palette, or write your own extraction from a downsampled bitmap for the challenge. Store it in a new nullable `BookEntity.coverColor` column: Room version 3 with `AutoMigration(2, 3)`, and a migration test. Work it out where covers are already extracted during a scan, not on the UI thread.
- `Spine`: the title rotated 90° and measured correctly (the rotated text must take up the rotated space). Width comes from file size in buckets; height from a stable hash of the title.
- `Shelf`: a custom `Layout` that bottom-aligns the spines on a plank and scrolls sideways when there are more.
- **Pull-out:** tapping a spine lifts it with `spatialBouncy` and shows its title, author and Open below the shelf. Tapping another swaps them; tapping the same one again pushes it back.
- Filter chips (All, Reading, Unread, Finished, with counts) filter the shelf and the grid.
- All books in a 3-column grid, with ribbons on the books in progress.

**Stretch:** the pulled spine turns (`rotationY`) to show its cover.

**Done when**
- The migration test passes.
- A fresh scan fills in the colors.
- The spines don't jump when one is pulled.
- TalkBack can reach and pull a spine.
- At 200% font size the spine titles are ellipsized, not clipped mid-glyph.

**You'll practise:** Room migrations, background work in the scanner, `Modifier.layout` for rotated measuring, custom `Layout`, and `updateTransition`.
- https://developer.android.com/training/data-storage/room/migrating-db-versions
- https://developer.android.com/develop/ui/compose/layouts/custom
- https://developer.android.com/develop/ui/views/graphics/palette-colors

**Think about**
- `Modifier.rotate(90f)` alone draws rotated but *measures* unrotated. Why does that break the shelf, and what has to happen in the `layout { }` block?
- A cover can change (when the user edits a book). When must `coverColor` be worked out again?

---

## Buffer · Mon 21 – Sun 27 Dec

Catch up, rest, or do Stretch items. A good time to fix the Should-fix items from R0–R3.

---

## R4 · Shelves, Series details, shared elements (due Sun 3 Jan)

**Goal:** the series screens, and covers that travel between screens.

**Boards:** Shelves, Series details. Motion note 5.

**Must**
- `SegmentedProgress` and `VolumeStepper`.
- The new series card. A Cards | Spines toggle, where Spines reuses `Shelf` with one spine per series. Sort stays.
- Series details: the hero in the series colour with the fanned covers, "Your progress", the volumes list with READ stamps (build a reusable `Stamp`), the folder chip, and an extended FAB that shrinks on scroll.
- **Shared elements:** wrap `NavDisplay` in `SharedTransitionLayout`. The fan on the series card becomes the fan on Series details, and a cover in a list becomes the cover on Book details.

**Stretch:** the fan opens wider on arrival (animate the angles after the transition).

**Done when**
- The transitions run both ways, including predictive back.
- Nothing flickers when you switch tabs mid-transition.
- The FAB state survives rotation.

**You'll practise:** `SharedTransitionLayout`, `Modifier.sharedElement` and `sharedBounds` with Navigation 3's animated content scope, and `derivedStateOf` on scroll state.
- https://developer.android.com/develop/ui/compose/animation/shared-elements

**Think about**
- The same book can be on Home twice (in the carousel and in the grid). What happens when two elements share one key?
- Why does `derivedStateOf` matter for "has the user scrolled past the first item?"

---

## R5 · Book details: the catalogue card (due Sun 10 Jan)

**Goal:** the most "Pustaka" screen.

**Boards:** Book details. Motion note 6.

**Must**
- The cover on a tinted arch (a shared element from R4), the series link, the title, and the author.
- "Continue from page n", and the bookmarks and contents button.
- Three stat tiles from `ReadingStats`.
- `CatalogueCard`:
  - a red top rule, and ruled mono fields drawn with `drawBehind`
  - a DATE READ column of `Stamp`s, one per local day with sessions, showing the last four
  - a punch hole, cut out rather than drawn on top
- The stamps **land one by one**: scale 1.6 to 1, rotation settling, a 40ms stagger. They start when the card first becomes visible, not when the screen opens.
- "Your notes", showing the latest highlight as a small index card.

**Stretch:** a light haptic as each stamp lands. Turn it off when reduce motion is on.

**Done when**
- The dates use the device's time zone.
- The stamps animate only once per visit.
- The punch hole shows the background behind it in both themes.

**You'll practise:** `drawBehind` and `drawWithContent`, `Path` operations or `BlendMode.Clear` with an offscreen `graphicsLayer`, `Animatable` with `LaunchedEffect`, and detecting when an item becomes visible.
- https://developer.android.com/develop/ui/compose/graphics/draw/overview
- https://developer.android.com/develop/ui/compose/animation/customize

**Think about**
- `BlendMode.Clear` without an offscreen layer clears more than you expect. Why?
- Where should "distinct days read" be worked out: SQL, the repository, or the ViewModel? Time zones should decide it.

---

## R6 · Notes: index cards, flip, quote card (due Sun 17 Jan)

**Boards:** Notes, Share as quote card. Motion notes 1 and 2 on the second sticky.

**Must**
- The Highlights | Bookmarks segmented button, the filter chips by book, and the export button.
- `IndexCard` in a `LazyVerticalStaggeredGrid`: coloured by highlight colour, rotated slightly with a stable angle per highlight, and with a folded corner when there's a note.
- **Flip:** `rotationY` from 0 to 180 with `cameraDistance`. The back (the note, Edit, Share card) shows past 90°, and isn't mirrored.
- **Quote card:** a composable drawn into a bitmap (`rememberGraphicsLayer` and `toImageBitmap()`), saved to the cache, and shared through a `FileProvider` content URI. Reuse what the Markdown export already does to share files.
- Cards arrive staggered the first time the screen opens.

**Stretch:** swipe a card away to delete it, with Undo.

**Done when**
- The flip runs at 60fps with no recomposition per frame. I'll check with Layout Inspector.
- The shared image is 1080 × 1350.
- TalkBack offers "Flip card" as a custom action.

**You'll practise:** staggered lazy grids, `graphicsLayer` 3D transforms, drawing a composable to a bitmap, and sharing with `FileProvider`.
- https://developer.android.com/develop/ui/compose/lists
- https://developer.android.com/develop/ui/compose/graphics/draw/modifiers

**Think about**
- The rotation has to be read in the *draw* phase so it doesn't recompose. How do you also switch front and back content without recomposing every frame?

---

## Buffer · Mon 18 – Sun 24 Jan

---

## R7 · Reading journal (due Sun 31 Jan)

**Boards:** Reading journal. Motion note 3 on the second sticky.

**Must**
- A pure, unit-tested `JournalStats`:
  - time read per year
  - books finished per year: the date is the end of the session that took the book past 99%. No schema change.
  - current and best streak (reuse R2's function)
  - average pages/hour
  - minutes per day for the heatmap and the last-7-days bars
- The route from the streak ticket (it replaces R2's placeholder), and a year picker.
- The totals count up from 0.
- The heatmap is drawn on a `Canvas` (not 112 composables): five levels, today ringed, future days dashed, filling in week by week.
- The bar chart grows from 0. Today is ribbon-coloured.
- The finished covers have FINISHED date stamps.
- A text summary of the heatmap for TalkBack.

**Stretch:** tap a heatmap day to see that day's minutes in a tooltip.

**Done when**
- The stats tests cover a year boundary, a book finished twice, and no data at all.
- Changing the year animates the numbers.

**You'll practise:** `Canvas` and `DrawScope`, animating drawn values, `animateIntAsState`, and semantics on custom drawings.

**Think about**
- 112 cells in one `Canvas` against 112 `Box`es: what does each cost at composition, layout and draw?

---

## R8 · Reader upgrades and the finished sheet (due Sun 7 Feb)

**Boards:** Reader: small upgrades, Reader: finished a volume. Motion notes 4 and 5 on the second sticky.

**Must** (only the items you adopted in D46)
1. The chapter chip, "n pages left in chapter", worked out from Readium positions and the table of contents. Also the session timer from `SessionTracker`.
2. Chapter ticks on the page slider (M3 `Slider` with a custom `track`). The thumb snaps to a tick within ±1 page, with a tick haptic.
3. The selection toolbar restyled as a pill, with the current colour ringed, Note, Copy, and Share as quote card (from R6).
4. The finished sheet: the FINISHED stamp landing with a haptic, the volume's totals, the next volume, and Back to library. It replaces the current finish card; keep its logic.

**Done when**
- The reader still turns pages smoothly; it doesn't recompose on scroll.
- The chip updates on page turns.
- The finished sheet appears once per finish.

**You'll practise:** the Slider's slot APIs, haptics, `ModalBottomSheet`, and working with Readium's `Locator` and positions.

**Think about**
- The reader is "quiet". Which of these changes could tempt you to animate while someone is reading, and how will you avoid it?

---

## R9 · Settings, accents, polish (due Sun 14 Feb)

**Boards:** Settings. Then every board, for the polish pass.

**Must**
- The library ticket with Rescan, the grouped lists, and the theme picker as mini previews.
- **Accent:** Ink, Forest, Plum or Rust. Store it in DataStore. Each accent supplies its own primary roles (light and dark) and passes the contrast test. Dynamic colours still overrides.
- **Accessibility pass:**
  - content descriptions
  - 48dp targets
  - 200% font scale on every screen
  - TalkBack order on Home and Book details
- **Motion pass:** with animator duration scale 0, nothing slides, flips or bounces.
- Empty states in the new style: no books, no notes, no sessions.

**Done when**
- I walk every screen in light, dark, all four accents, 200% font scale and reduce motion, and find no Must-fix items.

---

## R10 · Release 2.0.0 (due Sun 21 Feb)

- Bump the version to 2.0.0.
- Write `docs/releases/2.0.0.md`, and update the README features and screenshots (I can do this part).
- Run the full test suite and smoke-test a release build.
- Mark the redesign done in `docs/ROADMAP.md`.
- You sign and publish, as with 1.2.0.
