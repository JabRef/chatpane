---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# Message Bodies Through a Renderer Hook; Markdown with commonmark-java

## Context and Problem Statement

JabRef's AI chat — a possible first consumer (Carl) — shows answers as Markdown, rendered by its `MarkdownTextFlow` on flexmark.
Carl: "salvage usable MarkdownTextFlow parts … Introduce hooks when it makes sense."
How do message bodies get from text to what the layouts show, which Markdown parser does the library use, and how do bubbles show rich text?

## Considered Options

Rendering:
* A public `MessageRenderer` hook on the pane, turning a text into the library's own small model (`TextLine`, `TextSpan`: kinds and styles as CSS names, links as targets); built-ins `plainText()` (default) and `markdown()`
* Markdown only, always on
* A renderer producing incubator `RichParagraph`s directly

Parser:
* commonmark-java (+ GFM strikethrough)
* flexmark-java, as JabRef uses

Bubble bodies:
* A read-only `RichTextArea` per visible bubble, over the transcript's own model in a body-only format
* JabRef's approach: a `TextFlow` of `Text`/`Hyperlink` nodes with a hand-written selection (`SelectableTextFlow`)
* Keep plain `TextArea`s in bubbles, rich text only in the transcript

## Decision Outcome

Chosen options: **the renderer hook with the library's own model, commonmark-java, and `RichTextArea` bubble bodies**.

* The hook keeps the API free of incubator types (MADR 0010) and of any parser: an application can plug in a renderer on the parser it already has — JabRef could keep flexmark — and both views consume the same `TextLine`s, so a message reads alike in every layout.
  A second hook, `linkHandler`, opens links: the library does not know how an application opens a URL.
* commonmark-java ships real `module-info`s (`org.commonmark`, `org.commonmark.ext.gfm.strikethrough`), is BSD-2 and has no dependencies.
  flexmark 0.64 has neither `module-info` nor `Automatic-Module-Name` — only file-name-derived automatic module names, which `-Werror` and a library's consumers should not have to rely on.
* What is salvaged from `MarkdownTextFlow` (MIT, JabRef `b91795ad59`): its coverage and look — headings, emphasis, inline and block code, links, `•` bullets and numbered lists with nesting, block quotes, HTML as literal text, spacing between blocks — re-cut as a commonmark visitor that emits lines.
  Not salvaged: the flexmark visitor itself, its DI wiring, and copy-as-Markdown (reconstructing the source on *Copy*; a candidate for later).
  Deliberately different: a single line break stays a line break, as chat users expect.
* Bubbles use a `RichTextArea` like the transcript: selection, context menu and *Copy* stay JavaFX's own (MADR 0009's rule for text), and the body format is shared with the modern layout.
  A hand-written selectable `TextFlow` was rejected for re-implementing what a control provides; plain `TextArea`s in bubbles would have left Markdown out of the layout JabRef uses.

### Consequences

* Good, because JabRef's AI answers render in every layout, and JabRef can still bring its own renderer.
* Good, because the `TextArea` measuring (W1) and its wheel workaround (W2) are gone.
* Bad, because a `RichTextArea` per visible bubble is heavier than a `TextArea`, and it cannot shrink to its text while wrapping — the width is measured (W5).
* Bad, because counting a Markdown message's lines means parsing it (the transcript counts every message); a plain-text count stays cheap.
* Neutral, because styled segments are drawn with resolved styles, not with their style names (W3): tests find them by text.
