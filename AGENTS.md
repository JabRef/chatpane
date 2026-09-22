# ChatPane — Instructions for Agents

## Session protocol (suspend/resume)

* **At session start:** read `PLAN.md`. It holds current state, next actions, and milestone status. Do not re-derive the project state from scratch.
* **Before ending a session:** update `PLAN.md` — add a dated *Current state* entry on top, adjust *Next actions*, and check off milestone items.
* **Session history lives in git commit messages.**
  Write commit bodies that carry the narrative: what was done, why, what was learned or blocked.

## Conventions

* Build: `gradlew build traceRequirements` must be green before every commit.
  `gradlew uiTest` (needs a display; `just uitest` under Xvfb) when touching the skin, a cell or `chatpane.css`.
* Git: one worktree per task (workspace `CLAUDE.md`); integrate `main` with a merge, and when the push is rejected, `git pull --no-rebase` — **never rebase** (MADR 0004).
* Changelog: `CHANGELOG.md` in Keep a Changelog format, SemVer sections (MADR 0012).
  Every change to user-visible behavior (API, look, demo) adds a bullet under **Unreleased**.
  Verify before committing: `jbang heylogs@nbbrd check CHANGELOG.md` (rules in `heylogs.properties`); CI runs the same check.
* Releases (MADR 0012): `org.jabref:chatpane` on Maven Central, version in `gradle.properties` (`chatpaneVersion`, `-SNAPSHOT` between releases); `publish.yml` publishes snapshots of `main` and of each pull request (`<version>-PR<n>-SNAPSHOT`).
  To release: a pull request sets `chatpaneVersion` to the release version and renames *Unreleased* to `## [<version>] - <date>` (with a fresh empty *Unreleased* above and the links `.../compare/v<previous>...v<version>` and `.../compare/v<version>...main`); after it merged, `git tag v<version> <merge commit> && git push origin v<version>` runs `release.yml`, which refuses a tag that disagrees with `chatpaneVersion`; then a pull request sets the next `-SNAPSHOT`.
  The tag is a maintainer's call — never push one unasked.
* Requirements: OpenFastTrace in `docs/requirements/` (MADR 0001) — chain `feat → req → dsn → [impl->dsn~…~1]`/`[utest->dsn~…~1]` tags in code.
  A `dsn` item gains `Needs: impl` (and `utest` where sensible) in the same commit as the covering code.
  New behavior = new/updated spec item + tags in the same commit; bump the revision on semantic change and update every tag.
  OFT does not scan `.css`: tag stylesheet designs at the Java code that loads the sheet.
* Decisions: MADR in `docs/decisions/` (current MADR template, honest Considered Options, row in `docs/decisions/README.md`).
  Significant technology or architecture choices get a record; superseding is explicit.
* Java 25, JavaFX 26, Gradle Kotlin DSL; shared build setup in `build-logic/` (convention plugin `chatpane.java-conventions`), versions in `gradle/libs.versions.toml` (MADR 0002).
  Package root `org.jabref.chatpane`.
* Java Module System: both modules have a real `module-info.java`.
  The library exports API packages only (`org.jabref.chatpane`, `….skin`); anything else is a non-exported package.
  A new dependency needs a `requires` (or `requires static` for annotations) and must itself be a named or automatic module.
  `gradlew :demo:run` runs on the module path — the check that the descriptors still work.
  Tests have no `module-info` and run on the class path.
* Control architecture: follow `javafx.scene.control` (MADR 0009).
  The control holds state only (final `xProperty()`/`getX()`/`setX()` accessors, observable lists) and never builds child nodes; the skin (`….skin`, `SkinBase`, listeners via `registerChangeListener`/`registerListChangeListener`, cleanup in `dispose()`) renders it.
  Properties that shape the look are styleable (`CssMetaData` in a nested `StyleableProperties`, exposed via `getClassCssMetaData()`/`getControlCssMetaData()`, CSS name `-cp-…`).
  No FXML in the library; plain Java for the demo too.
* Doc comments in Markdown Javadoc form (JEP 467, `///`), not classic `/** … */`.
  Every public type and member of the library has one; it is the API documentation.
* Nullness: JSpecify (MADR 0006) — every package gets `@NullMarked` in `package-info.java`; anything nullable is annotated `@Nullable` in type-use position.
  Library: `compileOnlyApi` + `requires static transitive org.jspecify` (transitive because `@Nullable` is part of the public API).
* Logging: SLF4J 2 API everywhere (MADR 0005) — `private static final Logger LOGGER = LoggerFactory.getLogger(X.class);`, `{}` placeholders, no string concatenation.
  The library depends on `slf4j-api` only and never on a backend.
  tinylog 2 is the backend (`slf4j-tinylog` + `tinylog-impl`, `runtimeOnly`) in the demo and the tests only; demo config in `demo/src/main/resources/tinylog.properties`.
* Portability: keep all code OS-portable — no OS-specific APIs, paths via `user.home`/`Path`, no shell calls, locale-aware formatting (`DateTimeFormatter.ofLocalizedTime`), sizes in `em`.
  Windows is the development machine; CI builds on Linux.
* Licenses: the project is Apache-2.0.
  Runtime dependencies of the library must be permissive (Apache-2.0, MIT, BSD) — JavaFX itself (GPLv2 with Classpath Exception) is the accepted exception.
  Weak copyleft (EPL, MPL, LGPL, EUPL) only in test or build scope (e.g. TestFX's EUPL-1.2); strong copyleft never.
  Check the license of every new dependency and name it in the build-file comment.
  Code taken over from JabRef (MIT) keeps a `// Adapted from JabRef (MIT), <path>` line; add JabRef's copyright notice to `NOTICE` with the first such file.
* Unit tests for real logic (grouping, formatting, parsing), not for UI.
  Rendered behavior (cells per layout, pseudo-classes, CSS-settable properties) gets a TestFX test instead (`@Tag("ui")`, `gradlew uiTest`, MADR 0008) — not part of `build`.
  UI tests query nodes rather than drive the robot where they can (they run on the developer's desktop), and sort cells by `getIndex()`: the virtual flow keeps recycled cells in any order.
  A `RichTextArea` builds its text cells a pulse later and draws segments as plain `Text` nodes without their style names: `FxThread.settle(root)` before looking, and find shown text by its content.
* Docs: `README.md` is the user documentation — usage, the **CSS reference** (every style class, pseudo-class and CSS property the skin sets), demo, build.
  A new hook, CSS property or public API changes the README in the same commit.
  Docs drift: `CONSISTENCY.md` lists what prose must agree with code; `scripts/consistency.sh` runs the mechanical half — run it when touching `README.md` or `docs/`.
* Colors/theme (MADR 0007): the control brings **no palette and no theming layer** of its own and must look right when dropped into any application.
  Never a color literal in Java (no `Color.web`, hex strings, colors in `setStyle`) and none in `chatpane.css`: only the standard Modena lookups (`-fx-accent`, `-fx-base`, `-fx-background`, `-fx-control-inner-background`, `-fx-text-background-color`, `-fx-text-inner-color`; not `-fx-mid-text-color`, a fixed dark gray), with text contrast via Modena's own mechanism (set `-fx-background` on the container, labels use `-fx-text-background-color`).
  Expose state as pseudo-classes and structure as style classes so applications restyle with ordinary selectors; keep standard classes (`list-cell`, `label`) on standard nodes.
  The demo (an application) may define a theme for itself; the library never does.
* Workarounds: code that exists only because something upstream (JavaFX, a library, a tool) falls short gets a `Workaround W<n> (docs/workarounds.md)` comment at every place it touches and an entry in `docs/workarounds.md`: where, what upstream does, the upstream issue (link, or "none found"), and how to tell it can be removed.
  Search the upstream tracker (bugs.openjdk.org for JavaFX) before writing "none found".
  `scripts/consistency.sh` checks markers against entries; a removed workaround moves to the *Removed* list with the date, and its number is not reused.
* Incubator: every layout renders text with `jfx.incubator.richtext` (the transcript, MADR 0010; bubble bodies, MADR 0011) — consumers accept the incubator status.
  On a JavaFX upgrade, build, run the UI tests and look at the transcript in both demo themes; re-check the W-entries that concern the incubator.
* Markdown: one sentence per line (semantic line breaks) in all `*.md`, so diffs stay readable.
  Applies to new and edited text; do not mass-reflow untouched paragraphs.

## Architecture summary

Gradle multi-project: `:chatpane` — the library, module `org.jabref.chatpane`: `ChatPane` (the `Control`), `ChatMessage` (record), `MessageLayout` (`BUBBLES`/`IRC`/`MODERN`); `skin/ChatPaneSkin` maps each layout to a `ConversationView` (`EnumMap`) and reports message changes to the shown one (`MessageChanges`): `BubbleView` is a virtualized `ListView` whose `MessageCell` builds a bubble around a `BubbleText` body (a one-message `RichTextArea`), `TranscriptView` one read-only incubator `RichTextArea` over the virtual `TranscriptModel`; both are filled by a `TranscriptFormat` (`IrcTranscript`, `ModernTranscript`, `BodyFormat`, shared parts in `TranscriptSegments`) from the lines the pane's `MessageRenderer` makes (`plainText()`, `markdown()` on commonmark, in the non-exported `internal` package), read through one `RenderContext`; links go through `LinkInteraction` to the pane's link handler, the context menu and the pane's `MessageAction`s through `MessageMenu` (bubbles add hover buttons); a replaced message is updated in place (`MessageChanges.replacedAt` → `ConversationView.updated`); `MessageGrouping` decides which messages share a header; `chatpane.css` is the user-agent stylesheet.
`:demo` — module `org.jabref.chatpane.demo`, `DemoApp` with a sample conversation, layout toggles, a light/dark/system theme toggle (`DemoTheme`, `dark.css`) and an input line, logging SLF4J → tinylog.
Root build: OpenFastTrace over `docs/requirements`, `chatpane/src`, `demo/src`.
