# Changelog

All notable changes to ChatPane are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions are release dates ([CalVer](https://calver.org/), `YYYY-MM-DD`), tagged `v<date>` on GitHub once the day is over.
The topmost section is the day in progress — its link points at `main` until that tag exists.

## [2026-09-22] - 2026-09-22

### Added

- **`ChatPane`**, a JavaFX control showing a chat conversation in one of three layouts: talk bubbles left and right (`BUBBLES`), one line per message (`IRC`), or message by message with one header per group (`MODERN`). The layout can be switched at any time, from code or from CSS (`-cp-message-layout`).
- **Selectable message text**: select and copy with the standard context menu and shortcuts, as in any JavaFX text field.
- **Looks native in any theme**: the pane has no colors of its own and takes them from the application's JavaFX theme; the README's CSS reference lists every hook for restyling.
- **Named Java module** `org.jabref.chatpane`.
- **Demo application** with a sample conversation, a toggle per layout and an input line (`gradlew :demo:run`).
- **IRC and modern as one document**: select across messages and copy a stretch of conversation as a plain-text log (`15:34 <bob> text`). Uses JavaFX's incubator `RichTextArea`; an incoming message keeps your selection. The bubble layout is unchanged.
- **Demo: light, dark and system theme** toggle at the top right; the window decorations follow.
- **Markdown message bodies** in every layout: `ChatPane.setMessageRenderer(MessageRenderer.markdown())` — headings, emphasis, code, links, lists, quotes (commonmark-java). Plain text stays the default, and an application can plug in its own renderer.
- **Clickable links**: `ChatPane.setLinkHandler(…)` is called with a link's target; the demo opens it in the browser.
- **Demo: plain/Markdown toggle**, and a sample message showing what Markdown does.
- **Message actions**: `ChatPane.getMessageActions()` — each action in the message's context menu (every layout) and as a button next to a bubble under the pointer, limited to the messages it fits (`onlyFor`).
- **Message status** `SENT`, `PENDING`, `ERROR` (`ChatMessage.Status`), shown without colors of its own and exposed to CSS.
- **Live updates**: replacing a message updates it in place — a generated answer can grow while the user reads and selects.
- **Full date and time** as a tooltip on a bubble's time.
- **Demo: a pretend assistant** answers each message the way an AI chat does, with *Delete* and *Retry*.
- **Localizable texts**: `ChatPane.setTextLocalizer(…)` translates the pane's own texts (the context menu's *Copy* and *Select All*); JabRef can pass `Localization::lang`.
- **Time format as a pane property**: `ChatPane.timeFormatterProperty()` sets how times read, in every layout alike.

### Changed

- **`ChatMessage` takes a `Direction`** (`INCOMING`, `OUTGOING`) instead of a `boolean outgoing`; `MessageLayout.pseudoClassName()` is now `cssName()`, like `Direction.cssName()`.

### Fixed

- **IRC and modern are styled when first shown**: they no longer appear unstyled (italic, no colors, overlapping lines) until the text format is switched.
- **Switching the theme no longer crashes** with "duplicate children added", and IRC and modern follow the new theme's colors.

- **Selected text in a bubble is visible**: selecting in your own (accent-colored) bubble no longer paints the selection in the same color.
- **Times readable in dark themes**: message times use the dimmed text color instead of a fixed dark gray.
- **Long conversations in IRC and modern stay light**: the transcript builds only the lines on screen; 100 000 messages take about 6 MB instead of 107 MB.
- **Bubble text behaves like the transcript**: a bubble's body is a read-only rich-text area now, with the same selection, context menu, styles and links as IRC and modern.
- **Bubbles keep your selection**: a new message no longer scrolls the bubble layout away while you have text selected in it, as IRC and modern already did.

[2026-09-22]: https://github.com/calixtus/chatpane/commits/main
