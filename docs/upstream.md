# Upstream reports

What chatpane would like changed upstream, collected so it can be reported: bugs with a short reproduction, and feature requests.
Most concern JavaFX's incubator `RichTextArea` ([JDK-8344643](https://bugs.openjdk.org/browse/JDK-8344643)), whose maintainer takes reasonable feature requests.
A bug that chatpane works around has its details in [workarounds.md](workarounds.md) under the same `W<n>`; this list adds what to report.

Each entry: what to ask for, why chatpane needs it, a minimal reproduction or use case, and the status (not reported yet, or the issue link).
When an entry is reported, add the link here and in the workaround entry; when upstream fixes it, follow the workaround's "Removable when".

## Bugs

### U1 — `VFlow` moves the view the wrong way when lines above it are removed

* **What:** `VFlow.computeNewOrigin` shifts the origin by `endIndex - startIndex + linesAdded`; for a removal of `n` lines above the origin that is `+n` instead of `-n`, so the view moves `2n` lines down.
  The area's own deletions fire the same kind of event (`StyledTextModel.replace` → `fireChangeEvent`), so a multi-line delete above the visible lines of any `RichTextArea` is affected.
* **chatpane:** a message removed above the shown ones (an application pruning old history) moves the transcript by twice its lines.
  No workaround: the area has no public way to scroll back (F1); a caret placed on the old top line lands somewhere within a few lines.
* **Reproduce:** a model of 50 paragraphs, scroll so paragraph 30 is on top, delete paragraphs 2–3 (`fireChangeEvent(TextPos(2, 0), TextPos(4, 0), 0, 0, 0)` in a view-only model): the old paragraph 34 (now 32) is on top instead of the old paragraph 30 (now 28).
* **Status:** not reported yet.

### W6 — `RichTextArea` fails when a CSS pass sets its wrapping again

See [W6](workarounds.md#w6--richtextarea-fails-when-a-css-pass-sets-its-wrapping-again): "duplicate children added" from `VFlow.prepareCell`.
* **Status:** not reported yet (related: [JDK-8351982](https://bugs.openjdk.org/browse/JDK-8351982)).

### W8 — content height unknown until the first layout

See [W8](workarounds.md#w8--richtextarea-knows-its-content-height-only-after-its-own-layout): with `useContentHeight`, `prefHeight` before the area's first layout is `Params.LAYOUT_MIN_HEIGHT` plus insets; a parent that measures it then (a `ListView` cell) sizes it wrong for a pulse.
Ask: compute the arrangement on demand in `computePrefHeight(width)` (and give the area a horizontal content bias while wrapping, so the width is passed).
* **Status:** not reported yet.

### W9 — a caret moved by an edit scrolls into view

See [W9](workarounds.md#w9--richtextarea-scrolls-to-a-caret-that-an-edit-elsewhere-moved): an edit above the caret shifts its marker, and the area scrolls to the caret as if the user had moved it.
Ask: scroll to the caret on selection changes made through the API or input, not on marker shifts from model edits.
* **Status:** not reported yet.

## Feature requests

### F1 — a public scroll position

* **What:** read and set the vertical scroll position — e.g. the top paragraph and its pixel offset (the internal `Origin`), `scrollToParagraph(index, offset)`, scroll by pixels.
* **chatpane:** keeping the view in place across model changes (U1), following the newest message, and revealing a find match — today done by moving the (hidden) caret, which changes the user's selection.
* **Status:** not requested yet.

### F2 — shrink to the text and still wrap (W5)

* **What:** a preferred width of "the widest line, wrapping below the max width"; `useContentWidth` stops wrapping.
* **chatpane:** chat bubbles as wide as their text; today measured with hidden `Text` nodes ([W5](workarounds.md#w5--richtextarea-cannot-shrink-to-its-text-and-still-wrap)).
* **Status:** not requested yet.

### F3 — follow CSS changes in built paragraphs (W7)

* **What:** re-resolve segment style names when the scene's CSS changes (a theme switch), or a public call to do so.
* **chatpane:** a style probe triggers `RichTextAreaSkin.refreshLayout()` ([W7](workarounds.md#w7--richtextarea-resolves-style-names-once-per-cell-and-never-again)).
* **Status:** not requested yet.

### F4 — styled segments take the area's text fill (W3)

* **What:** a segment added with style names draws as a plain `Text` with resolved styles; the area's `-fx-text-fill` reaches none of them.
* **chatpane:** every text rule repeats `-fx-fill` ([W3](workarounds.md#w3--richtextarea-draws-styled-segments-with-resolved-styles-not-the-areas-text-fill)).
* **Status:** not requested yet.
