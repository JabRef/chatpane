# ChatPane — Working Plan

## How to resume (read this first)

1. Read **Current state** below, then pick the top unchecked item in **Next actions**.
2. Conventions: `AGENTS.md`; requirements in `docs/requirements/` (OpenFastTrace), decisions in `docs/decisions/` (MADR); `gradlew build traceRequirements` must be green before every commit.
3. Before stopping: add a dated **Current state** entry on top, update **Next actions**, check off finished **Milestones** items.
   Session history lives in the git commit messages.

## Current state (M0 done, M1 in progress)

*2026-09-23* — **Maven Central publishing, SemVer proposed (MADR 0012, supersedes 0003).**
Oliver: "Port the maven central publishing flow from JabRef/html-to-node"; then "switch to semver — and start with 0.1.0 — the -SNAPSHOTs are nice, no nightly thing".
vanniktech `maven-publish` on `:chatpane` as `org.jabref:chatpane`; `publish.yml` pushes `0.1.0-SNAPSHOT` from `main` (PRs: `0.1.0-PR<n>-SNAPSHOT`), `release.yml` releases a `vX.Y.Z` tag. Changelog: the CalVer day sections, never tagged, are one `[Unreleased]` section now.
Needs the `KOPPOR_*` organization secrets to be available to this repository.

*2026-09-22 (5)* — **Conventions moved to `AGENTS.md`.**
Carl: "do it with a CLAUDE.md stub importing AGENTS.md".
Merged the open `rename-claude-to-agents` branch; `CLAUDE.md` is now one line, `@AGENTS.md`, so Claude Code still loads the conventions and other agents read them directly.

*2026-09-22 (4)* — **Rebranded to `org.jabref.chatpane`.**
Carl: "Rebrand this project from io.github.calixtus.chatpane to org.jabref.chatpane".
Packages, modules (`org.jabref.chatpane`, `org.jabref.chatpane.demo`), the stylesheet resource path and all docs follow; MADR 0002 now names `org.jabref` as the natural Maven group.
Nothing was released yet, so the changelog's module bullet was edited in place rather than logged as a change.
The GitHub repository stays `calixtus/chatpane`; its URLs are unchanged.

*2026-09-22 (3)* — **All code-review findings implemented (below, "Code review").**
Carl: "implement all findings, view split first".
Three commits: the view split (`ConversationView` with `BubbleView`/`TranscriptView`, an `EnumMap` in the skin, `TranscriptFormat` with `IrcTranscript`/`ModernTranscript`, `MessageChanges` with a unit test, one follow-the-newest rule — bubbles no longer jump away from a selection, `FollowNewestUiTest` red without it); `ChatMessage.Direction` instead of `boolean outgoing`, `MessageLayout.cssName()`; the rest (time formatter as a pane property, `EnumMap`/one update method in `ChatPane`, final skin, `dispose` test, `FxThread` test helper, demo labels, test versions from the catalog).
Carl, mid-task: integrating this as a replacement for JabRef's AI chat is a possibility — Next actions are ordered by what that needs.
Then, per Carl: transcript virtualization, Markdown from `MarkdownTextFlow`, message actions and the other usable JabRef chat parts, with hooks where they make sense.
Markdown done (MADR 0011): a public `MessageRenderer` hook producing the library's own `TextLine`/`TextSpan` model (`plainText()` default, `markdown()` on commonmark-java — flexmark has no module names), after `MarkdownTextFlow`'s coverage; a `linkHandler` hook; bubble bodies are one-message `RichTextArea`s over the transcript model in a shared `BodyFormat`, so both views render alike. That retired W1 and W2 (a content-high `RichTextArea` passes the wheel on — checked by `WheelOverBodyUiTest` without the filter) and added W5 (the bubble measures its own width). Found on the way: the area resolves style names into styles and draws plain `Text` nodes — tests find segments by text (W3 extended).
Localization hook done (`textLocalizer`, English text in; JabRef passes `Localization::lang`). Two bugs Carl found in the demo, both upstream in the incubator `RichTextArea`: the transcript showed unstyled when first shown (style names resolved once per text cell, before the CSS was there) and switching to dark crashed ("duplicate children added": `-fx-wrap-text` set again during a CSS pass). Fixed around them: the area's flags in code (W6), a `StyleProbe` that redraws the text when CSS changes (W7). `TranscriptStylingUiTest` reproduced both, fails without either fix. Carl got a bug summary for the RichTextArea author.
Message actions, status and live updates done: `MessageAction` hook (context menu in every layout via `MessageMenu`, hover buttons on bubbles as in JabRef), `ChatMessage.Status` (`SENT`/`PENDING`/`ERROR`, no color of its own — Modena has no error lookup), `set(index, …)` updates one message in place (`MessageChanges.replacedAt`, `ConversationView.updated`, `TranscriptModel.update` with an exact change event; a rebuild only if the next message's grouping changes), full date/time tooltip on bubbles; the demo's pretend assistant streams, fails and retries.
Transcript virtualization done: `TranscriptModel` keeps the messages and one paragraph offset each and builds paragraphs on demand (256 messages cached). Probe, 100 000 messages: IRC +107 MB → +6 MB (bubbles: +4 MB), show 238 → 119 ms, layout switch 168 → 53 ms.

*2026-09-22 (2)* — **IRC and modern as one RichTextArea transcript; demo theme switch; workaround registry (MADR 0010).**
Carl: "implement RichTextArea for IRC and normal if possible. Only consumer planned so far is only JabRef. Others must accept current incubation status."
`ChatPaneSkin` now swaps two views: `BUBBLES` stays the `ListView` (a bubble is a region around text, which paragraph attributes cannot draw), `IRC`/`MODERN` are one read-only incubator `RichTextArea` over `TranscriptModel` (view-only `StyledTextModel`, appends fire exact change events so scroll position and selection survive; any other list change rebuilds), filled by `TranscriptParagraphs` (CSS style names only, no colors).
An incoming message scrolls the transcript only while nothing is selected. The incubator is a plain `requires` of the core module — no optional module, per Carl.
Also, from Carl mid-task: the demo's light/dark/system toggle top right (`DemoTheme` via `Scene.Preferences.colorScheme`, so the title bar follows; `dark.css` is the demo's own Modena override); bubble selection was invisible (the focused highlight is `-fx-accent`, an outgoing bubble's own color) — now accent in incoming, inverted in outgoing bubbles, `MessageTextUiTest.selectionContrastsWithItsBubble` red without the fix; every workaround for an upstream shortfall now has a `Workaround W<n>` marker and an entry in `docs/workarounds.md` (W1–W4), checked by `consistency.sh`.
Found by the dark theme: transcript text stayed black (styled segments ignore the area's text fill — W3), times used Modena's fixed dark `-fx-mid-text-color`.
Checked by screenshot: all layouts in light and dark (the modern-dark shot was partly covered by another window).
Next: Carl's review request — clean code, SOLID, Effective Java, symmetry.

*2026-09-22 (1)* — **Project skeleton.**
Carl: a skeleton with rules for decisions (MADR), requirements, PLAN.md, changelog, Git, build, nullness, logging (SLF4J API with tinylog backend), portability, licenses, unit tests, docs, colors/theme and Markdown; a library with a JavaFX chat pane whose message layout the user chooses (bubbles, IRC, modern), a demo app, Java Module System.
Mid-task additions by Carl: follow the standard JavaFX control architecture (Control + skin, MADR 0009); no theming layer of its own, CSS hooks by convention, look fine when dropped into any application (MADR 0007); look at JabRef's AI chat for what can be salvaged (below); reuse JavaFX text controls so selection/copy work out of the box (message bodies are read-only `TextArea`s, `dsn~message-text~1`); input field and send button belong to the demo, the pane only displays (`req~display-only~1`); maybe the incubator `RichTextArea` for IRC (Next actions 3).
Built: Gradle Kotlin DSL with a `build-logic` convention plugin, `:chatpane` (module `org.jabref.chatpane`) and `:demo` (module `….demo`, runs on the module path, SLF4J → tinylog verified); `ChatPane`/`ChatMessage`/`MessageLayout`, `ChatPaneSkin` on a `ListView`, `MessageCell` for the three layouts, `MessageGrouping`; styleable `-cp-message-layout`; `chatpane.css` from Modena lookups only.
Tests: `MessageGroupingTest`, `ChatMessageTest`; `ChatPaneUiTest` (TestFX: default layout, CSS-set layout, groups, bubble sides, IRC lines), `MessageTextUiTest` (a body's text wraps at the area's full width and fits its height — both were broken at first: an as-needed scroll bar stole 12 px, the line height was 1 px short).
MADRs 0000–0009, requirements `features.md`/`chat-pane.md`/`demo.md`, CHANGELOG, CONSISTENCY.md + `scripts/consistency.sh`, CI (`build.yml`: build + trace, heylogs, UI tests under Xvfb).
Checked by screenshot in stock Modena: all three layouts render, long text wraps, groups share a header.
First CI run (`fb9e3ee`) green: build + trace, changelog, UI tests under Xvfb on `ubuntu-latest`.
Not checked: a dark theme.

## Next actions

1. JabRef migration sketch: `AiChatView` keeps its input (`HistoryTextArea`), status panes, follow-up questions and privacy notice; `ChatPane` replaces `ListScrollPane` + `AiChatMessageView` — roles USER/AI/ERROR map to `Direction` plus `Status.ERROR`, delete/regenerate become `MessageAction`s, `MarkdownTextFlow` becomes `MessageRenderer.markdown()` (or a flexmark renderer of JabRef's own), the hyperlink handler becomes `linkHandler`.
2. Hanging indent for wrapped IRC lines (paragraph `SPACE_LEFT` + negative `FIRST_LINE_INDENT`, if the incubator supports it).
3. Copy as Markdown (as `MarkdownTextFlow` does): *Copy* of a Markdown message puts its source on the clipboard next to the plain text.
4. Demo dark theme: a selected toggle is hard to tell from the others.

## Code review (2026-09-22, after `f19a756`)

Clean code, SOLID, Effective Java (EJ), symmetry — **all implemented** (2026-09-22 (3)).

*Symmetry*
- S1 The two views are built unevenly: the transcript has its own model and paragraph builder, the bubble view is inlined in `ChatPaneSkin` (list setup, scroll filter, `scrollListToEnd`). Extract `BubbleView` and `TranscriptView` behind one package-private interface (`node()`, `rebuild(messages)`, `appended(messages, from)`, `detach()`), chosen from an `EnumMap<MessageLayout, …>` — also fixes S2/S3 and SOLID-1/2.
- S2 Follow-the-newest differs: the transcript scrolls only while nothing is selected, bubbles always jump to the end. One rule for both ("follow while the user is at the end and not selecting").
- S3 "Which layouts are a transcript" is known twice: `layout == BUBBLES` in the skin and the constructor check in `TranscriptParagraphs`.
- S4 The model says `boolean outgoing`, CSS says `:incoming`/`:outgoing`: an enum `Direction { INCOMING, OUTGOING }` in `ChatMessage` (also Clean Code's flag argument: `new ChatMessage("me", "x", t, true)`) — cheap before the first release.
- S5 Time formatting: the transcript gets it injected (testable), the cells call `MessageFormats` directly. One formatter for both, ideally a pane property (the application picks the format).
- S6 Demo toggles: layouts are labelled by `MessageLayout.cssName()` (a CSS name used as UI text), themes by `DemoTheme.label()`; give layouts a demo label too.
- S7 Grouping marks: bubbles expose `:continued`, transcript segments carry no `continued` style name.

*SOLID*
- SOLID-1 (SRP) `ChatPaneSkin` does view switching, change classification, transcript sync, bubble scrolling and Workaround W2 — S1.
- SOLID-2 (OCP/LSP) `TranscriptParagraphs` switches on the layout with a throwing `BUBBLES` branch and a constructor guard: one small strategy per transcript layout instead.
- SOLID-3 (single source of truth) `ChatPaneSkin.transcriptModel` duplicates `transcript.getModel()`.

*Effective Java*
- EJ-19 `ChatPaneSkin` is public and non-final (for replacement via `-fx-skin`/`setSkin`) but has no protected hooks: document it as not designed for subclassing, or offer hooks (e.g. a cell factory).
- EJ-37 `ChatPane.LAYOUT_PSEUDO_CLASSES` is a `Map` from a stream: an `EnumMap`.
- DRY in `ChatPane`: the constructor sets the default pseudo-class by hand, `invalidated()` does the same logic — one `updateLayoutPseudoClasses()`.
- `MessageFormats.time` builds a formatter per call (per cell update); cache per locale/zone if S5 does not replace it.

*Tests and build*
- `ChatPaneSkin.appendedFrom` (pure change classification) has no unit test; `dispose()` has none either.
- `onFx` is copied into both UI test classes: one test helper.
- The convention plugin repeats versions from `libs.versions.toml` (tinylog, jspecify): read the catalog there (`versionCatalogs.named("libs")`).

## Salvage from JabRef's AI chat

Source: `jabref/jabgui/src/main/java/org/jabref/gui/ai/chat/` and `…/gui/util/component/` (JabRef `b91795ad59`, 2026-09-15), MIT — compatible with Apache-2.0; copied code keeps an attribution line and JabRef's notice goes into `NOTICE` (AGENTS.md, *Licenses*).
JabRef's chat is an application view (FXML + afterburner injection + view models, `StackPane` subclasses), not a control; what carries over are its components and ideas, re-cut to the control/skin shape (MADR 0009).

| JabRef piece | What it does | Verdict |
|--------------|--------------|---------|
| `util/component/MarkdownTextFlow` (554 lines, flexmark) | Renders Markdown (emphasis, code, lists, block quotes, links) into a selectable `TextFlow`; hyperlink handler as a `Consumer<String>` property; plain-text mode | **Salvage** as the message-body renderer: strip `DialogService`/`ClipBoardManager`/`GuiPreferences`/afterburner (`Injector`) and JabRef's `SelectableTextFlow` (port or replace), keep the flexmark visitor. flexmark-java is BSD-2 and ships automatic module names — check JPMS fit before committing to it. Make the renderer pluggable (plain text default, Markdown opt-in) so the core module need not depend on flexmark: likely a second module `chatpane-markdown`. |
| `util/component/HistoryTextArea` (155 lines, pure JavaFX) | Input `TextArea`: Enter submits, Shift+Enter newline, Up/Down walk the history like a shell, grows up to 10 rows | **Salvage into the demo** as its input line. Not into the library: the pane only displays messages (Carl, `req~display-only~1`); input belongs to the application. |
| `util/component/ListScrollPane` (209 lines) | `VBox` in a `ScrollPane` with a renderer function and auto-scroll to bottom | **Not salvaged**: non-virtualized, rejected in MADR 0009; its auto-scroll-unless-the-user-scrolled-up rule is worth copying into `ChatPaneSkin` (today it always scrolls on append). |
| `AiChatMessageView` + `.fxml` | Bubble with sender label and Markdown body; alignment right for the user; pseudo-classes `:user`/`:ai`/`:error`; hover-only action buttons (delete, regenerate); context menu *Copy*; timestamp tooltip | **Ideas**: message actions as an API (`onDelete`-style event handlers or an action list on the pane), context menu with *Copy*, an `:error` state (`ChatMessage` gains a status/kind), hover-revealed action column. Not the FXML/afterburner wiring. |
| `.chat-bubble` rules in `jabref-base.css` | Bubble radius/border/padding, colors from JabRef's `-color-ai-message-*` variables | **Not salvaged**: a theme-specific palette, exactly what MADR 0007 rules out. JabRef maps its variables onto our hooks when it adopts the pane. |
| `AiChatView` follow-up questions (`SimpleListView` of buttons) | Suggested replies under the conversation | **Later idea**: "suggestion chips" hook; not a core feature. |
| `AiChatViewModel`, status/privacy/ingestion views, group/entry chat windows | AI-, BibEntry- and JabRef-specific orchestration | **Not salvaged.** |

Consequence for JabRef: once M1 lands, `AiChatView` could replace `ListScrollPane` + `AiChatMessageView` with `ChatPane` (`BUBBLES`, Markdown renderer, actions) — the salvage should keep that migration path in mind.

## Milestones

### M0 — Skeleton

- [x] Gradle build (Kotlin DSL, `build-logic`, version catalog), two named modules, demo on the module path
- [x] Conventions (`AGENTS.md`), MADRs 0000–0009, requirements with OpenFastTrace, CalVer changelog with heylogs, consistency checks, CI
- [x] `ChatPane` control + skin with the three layouts, grouping, styleable layout, Modena-only stylesheet
- [x] Unit and UI tests; demo with layout toggles and input line

### M1 — Rich messages

- [x] Dark-theme check in the demo (light/dark/system toggle)
- [x] Pluggable message-body renderer (`MessageRenderer`); Markdown on commonmark after JabRef's `MarkdownTextFlow` (MADR 0011)
- [x] Selectable, copyable text via read-only `TextArea` bodies (standard context menu and shortcuts)
- [x] IRC and modern on the incubator `RichTextArea` (MADR 0010, in the core)
- [x] Message actions API (context menu, hover buttons), message status (`:pending`, `:error`), live updates
- [ ] Auto-scroll only while at the bottom

### M2 — Polish

- [ ] Demo input line from JabRef's `HistoryTextArea` (history, Shift+Enter)
- [ ] Avatars / per-sender colors as data (MADR 0007 exception)
- [ ] Date separators, unread marker
- [ ] Accessibility (roles, texts for screen readers)
- [x] Publishing: Maven Central coordinates, POM without JavaFX classifiers, SemVer question (MADR 0012)
