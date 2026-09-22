---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# IRC and Modern Layouts as One RichTextArea Transcript (Incubator)

## Context and Problem Statement

The first version rendered every layout as `ListView` cells with one read-only `TextArea` per message (MADR 0009).
Selecting and copying works inside one message, but not across messages — and for IRC and the modern layout, which read as a continuous log, copying a stretch of conversation is the natural thing to do.
JavaFX 24 added `RichTextArea` as an incubator module (`jfx.incubator.richtext`); Carl: "implement RichTextArea for IRC and normal if possible. Only consumer planned so far is only JabRef. Others must accept current incubation status."
How are the IRC and modern layouts rendered?

## Considered Options

* One read-only `RichTextArea` over a custom `StyledTextModel` for IRC and modern ("the transcript"); bubbles stay on the `ListView`
* `RichTextArea` for all three layouts, bubbles as paragraph backgrounds
* Keep per-message `TextArea`s in a `ListView` for every layout (status quo)
* The transcript in an optional module (`org.jabref.chatpane.richtext`), the core incubator-free

## Decision Outcome

Chosen option: **one read-only `RichTextArea` transcript for IRC and modern, bubbles unchanged, in the core module**.

* The whole conversation is one document: a selection runs across messages, and *Copy* exports plain text — for IRC a log of `time <sender> text` lines.
  Selection, context menu and shortcuts are the area's own, as MADR 0009 asks of text.
* Colors stay out of the model: segments carry CSS style names only (`message-sender`, …), which `chatpane.css` styles from Modena lookups (MADR 0007).
* The model (`TranscriptModel`, on `StyledTextModelViewOnlyBase`) is a list of prebuilt paragraphs that grows by appending and fires exact change events, so an incoming message neither loses the scroll position nor the user's selection; any other change of the message list rebuilds it.
* Bubbles stay a `ListView`: a talk bubble — rounded, right-aligned, shrunk to its text — is a region around text, which paragraph attributes (backgrounds, spacing) cannot draw.
* In the core, not an optional module: the only planned consumer, JabRef, accepts the incubator, and anyone else must too (Carl); an optional module would mean a second skin and a service lookup for nothing.
  The dependency is `requires jfx.incubator.richtext`, not transitive — no incubator type is part of the API — so a modular consumer resolves it automatically and a classpath consumer needs nothing extra.

### Consequences

* Good, because IRC and modern become copyable as text, the behavior users of those layouts expect.
* Good, because one document costs one control instead of one `TextArea` per visible message.
* Bad, because the incubator API may change or be removed between JavaFX releases (JEP 11): every JavaFX upgrade needs a build and a look at the transcript, and the module prints an incubator warning at startup.
* Bad, because the skin now has two views to keep in sync with the message list; only the visible one is kept up to date.
* Neutral, because styled segments ignore the area's text fill (Workaround W3 in `docs/workarounds.md`) — the kind of rough edge an incubator has.
