# Workarounds

Code that exists only because something upstream (JavaFX, a library, a tool) does not do what it should or could.
Each entry says where the workaround lives, what upstream does, which upstream issue tracks it, and how to tell that it can go.
In code, every workaround carries a `Workaround W<n>` marker naming its entry here, so `git grep "Workaround W3"` finds all of it; `scripts/consistency.sh` checks that markers and entries match.

When upstream fixes one: remove the code at every marker, delete the entry (the number is not reused), and note it in `CHANGELOG.md` if behavior changes.

## Removed

* **W1** — `TextArea` does not size itself to its content ([JDK-8091714](https://bugs.openjdk.org/browse/JDK-8091714), [JDK-8181766](https://bugs.openjdk.org/browse/JDK-8181766)): `MessageText` measured its text.
  Gone 2026-09-22 with the `TextArea` bodies; a bubble body is a `RichTextArea` with `useContentHeight` now (W5 is the narrower remainder).
* **W2** — `TextArea`'s inner `ScrollPane` swallowed wheel events: `BubbleView` redirected them to the list.
  Gone 2026-09-22 with the `TextArea` bodies: a content-high `RichTextArea` passes wheel events on (`WheelOverBodyUiTest` passes without the filter).

## W3 — RichTextArea draws styled segments with resolved styles, not the area's text fill

* **Where:** `chatpane.css`, rules `.chat-pane .chat-pane-transcript .message-text` and `.chat-pane .message-bubble .message-text` (`-fx-fill`).
* **Upstream:** a segment added with `RichParagraph.Builder.addWithStyleNames` is not a `Text` carrying those names: the area resolves the names through the stylesheet into styles and draws a plain `Text` with them (seen in the node tree: the shown `Text` has no style class).
  The area's own `-fx-text-fill` (Modena: `-fx-text-inner-color`) reaches no segment — a standalone check (2026-09-22) showed a plain segment and a style-named one both black under `.rich-text-area { -fx-text-fill: red }` — so without an explicit `-fx-fill` the text stays black, unreadable in a dark theme.
  Consequence for tests: find shown segments by their text, not by style class.
  No upstream issue found; `jfx.incubator.richtext` (26.0.2) is an incubator module, tracked in general by [JDK-8351982](https://bugs.openjdk.org/browse/JDK-8351982).
* **Removable when:** transcript and bubble text read light in the demo's dark theme (`just demo --layout=irc --theme=dark`) with the `-fx-fill` rules removed.

## W4 — OpenFastTrace does not scan CSS

* **Where:** the `[impl->dsn~chatpane-stylesheet~…]` tag sits on `ChatPane.getUserAgentStylesheet` instead of in `chatpane.css`.
* **Upstream:** OpenFastTrace's tag importer has no `.css` file type.
  No upstream issue filed.
* **Removable when:** OpenFastTrace imports tags from `.css` files; then the tag moves into the stylesheet's header comment.

## W5 — RichTextArea cannot shrink to its text and still wrap

* **Where:** `BubbleText` (`computePrefWidth` and the measuring `Group` of `Text`s the cell puts into the bubble).
* **Upstream:** `useContentWidth` makes the preferred width the content width but stops wrapping — a long message becomes one cut-off line (probe 2026-09-22: preferred 694 px, shown 336 px, one line); without it, a short message gets a full-width bubble.
  The body therefore measures its widest line itself, with `Text` nodes that carry the spans' style names inside the bubble, so the stylesheet gives them the fonts the area's text gets.
  No upstream issue found; related: [JDK-8310593](https://bugs.openjdk.org/browse/JDK-8310593) (useContentWidth/Height for scrollable controls), [JDK-8344643](https://bugs.openjdk.org/browse/JDK-8344643) (RichTextArea).
* **Removable when:** a `RichTextArea` offers "preferred width = widest line, wrap below the max width"; then `BubbleText` sets it and drops its measuring.
  Check: `BubbleTextUiTest.bodyIsAsTallAsItsTextAndShrinksToShortText`.

## W6 — RichTextArea fails when a CSS pass sets its wrapping again

* **Where:** `TranscriptView` and `BubbleText` set `wrapText`, `useContentHeight`, `displayCaret` and `highlightCurrentParagraph` in code; `chatpane.css` no longer sets them.
* **Upstream:** with `-fx-wrap-text` coming from the pane's user-agent stylesheet, adding a stylesheet to the scene (the demo's dark theme) re-applies CSS, the property changes in the middle of that pass, and the area's `VFlow.handleWrapText` lays out right away: `IllegalArgumentException: Children: duplicate children added: parent = Pane[styleClass=content]` from `VFlow.prepareCell` (Carl, 2026-09-22; `TranscriptStylingUiTest` reproduced it).
  A standalone check with the wrapping in an author stylesheet did not fail, so the trigger needs the property to change during the pass.
  No upstream issue found; tracked in general by [JDK-8351982](https://bugs.openjdk.org/browse/JDK-8351982).
* **Removable when:** the settings move back into `chatpane.css` (so applications can style them) and `TranscriptStylingUiTest` passes, with no exception on the FX thread.

## W7 — RichTextArea resolves style names once per cell and never again

* **Where:** `StyleProbe` in `ChatPaneSkin` (styled from every lookup the text uses, in `chatpane.css`) and `ConversationView.restyle()` — the transcript calls `RichTextAreaSkin.refreshLayout()`, bubbles rebuild their cells.
* **Upstream:** the area turns a segment's style names into styles when it builds the text cell (`VFlow.resolveStyles` measures a probe `Text`) and keeps the cell. A later CSS change — another theme, a stylesheet added — does not reach built cells (standalone check: a segment named `tinted` stayed red after the stylesheet switched `.tinted` to blue), and cells built before the pane's CSS was in place keep the defaults (Carl, 2026-09-22: the transcript "shows initially without styling" until the renderer is switched, which builds a new model).
  No upstream issue found.
* **Removable when:** the area re-resolves styles on CSS changes itself; check: `TranscriptStylingUiTest` passes with the probe's callback doing nothing.
