---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# Standard JavaFX Control Architecture: Control, Skin, CSS, Virtualized List

## Context and Problem Statement

ChatPane is a reusable control, not an application view.
Carl: "This control should follow Oracle JFX standards for architecture of a JFX control (with skin etc)."
How is it structured, and what renders the message list?

## Considered Options

Structure:
* `Control` + `SkinBase` skin + user-agent stylesheet + `CssMetaData`, as in `javafx.scene.control`
* A `Region`/`StackPane` subclass building its children directly (JabRef's `AiChatView`, FXML + view model)

Message list:
* `ListView` with a custom `ListCell` (virtualized)
* `VBox` in a `ScrollPane`, one node per message (JabRef's `ListScrollPane`)
* A bare `VirtualFlow`

## Decision Outcome

Chosen options: **`Control` + skin, with a `ListView` inside the skin**.

The control/skin split is the contract JavaFX users know:
* `ChatPane extends Control` holds state only — properties, the message list — and no child nodes; final property accessors (`xProperty()`, `getX()`, `setX()`).
* The skin, `org.jabref.chatpane.skin.ChatPaneSkin extends SkinBase<ChatPane>`, builds and lays out the nodes; it is public in an exported `skin` package (as `javafx.scene.control.skin` is since JavaFX 9) so an application can replace it via `setSkin` or `-fx-skin`.
  *Amended 2026-09-22:* the skin is `final` — it has no hooks to override (Effective Java item 19), and customization goes through the pane's properties and CSS; subclassing was never really possible.
  Listeners are registered through `SkinBase.registerChangeListener`/`registerListChangeListener` so `dispose()` removes them.
* The default look is the user-agent stylesheet `chatpane.css` (MADR 0007); state visible to CSS is exposed as style classes and pseudo-classes, and behavior-shaping properties are styleable (`-cp-message-layout`, via `getClassCssMetaData()` / `getControlCssMetaData()`).
* Behavior classes (key handling, like `javafx.scene.control.behavior`) are not public API in JavaFX; when the pane needs input handling it lives in a package-private class next to the skin.
* No FXML inside the library: it would add `javafx.fxml` to every consumer's module graph.

`ListView` because chat histories grow without bound and a virtual flow keeps only the visible rows as nodes; `ListCell` recycling is also what `javafx.scene.control` users expect.
`ListScrollPane` (VBox) was rejected despite its simplicity and exact variable row heights: one node tree per message does not scale to long conversations.
A bare `VirtualFlow` would avoid `ListView`'s selection and focus machinery, but needs its own cell plumbing; the skin hides the choice, so it can be swapped later without an API change.

### Consequences

* Good, because consumers can restyle, subclass the skin, or replace it without forking.
* Good, because long histories cost only the visible rows.
* Bad, because `ListView`'s selection and focus look has to be neutralized in CSS (a chat row is not a pickable item), and variable-height wrapped cells in a `VirtualFlow` can jitter while scrolling — to watch as messages get richer (Markdown, images).
