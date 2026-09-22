# Chat pane

The control, its message model and its default skin.

## Requirements

### Show a conversation
`req~show-conversation~2`

An application adds messages — sender, text, time sent, and whether the local user wrote it — to the pane, and the pane shows them in the order given, scrolled to the newest one whenever the messages change — in every layout alike, unless the user has text selected in the pane, which a new message never takes away.
Long messages wrap within the pane's width; the pane never scrolls horizontally.

Covers:
- feat~chat-pane-control~1

Needs: dsn

### Display only
`req~display-only~1`

The pane only displays messages.
Composing and sending — an input field, a send button — belong to the application, which appends what it sends to the pane's messages like any other message.

Covers:
- feat~chat-pane-control~1

Needs: dsn

### Standard text actions
`req~standard-text-actions~1`

The user selects message text with the mouse or keyboard and copies it with the platform's usual means — the standard context menu (*Copy*, *Select All*) and shortcuts — exactly as in any other JavaFX text control, without the application doing anything.
Scrolling the mouse wheel over a message scrolls the conversation.

Covers:
- feat~chat-pane-control~1

Needs: dsn

### Select across messages
`req~select-across-messages~1`

In the IRC and modern layouts the conversation reads as one text: a selection can run from one message into the next, and copying it gives a plain-text log — for IRC `15:34 <bob> text` per line.
Covers:
- feat~chat-pane-control~1
- feat~selectable-message-layouts~1

Needs: dsn

### Rendered message text
`req~rich-text~1`

The application chooses how message texts are rendered — plain (the default), Markdown (headings, emphasis, code, links, lists, quotes; HTML shown as text), or with its own renderer — and a message reads the same in every layout.
Clicking a link hands its target to the application, which decides how to open it; without a handler links are shown but inert.

Covers:
- feat~rich-message-text~1

Needs: dsn

### Message actions
`req~message-actions~1`

The application offers actions on single messages — delete, answer again, … — each shown only for the messages it applies to.
The user finds them in the message's context menu in every layout, after *Copy* and *Select All*, and in the bubble layout also as buttons next to the bubble under the pointer.

Covers:
- feat~chat-pane-control~1

Needs: dsn

### Message status
`req~message-status~1`

A message can be pending (still being sent or generated) or failed; the pane marks both visibly in every layout, without a color of its own, and exposes them to CSS so the application can color them.

Covers:
- feat~chat-pane-control~1

Needs: dsn

### Live update
`req~live-update~1`

When the application replaces a message — an answer that grows while it is generated, a status that changes — the pane updates that message in place: the rest of the conversation, the scroll position and the user's selection stay; the view follows it if it is the newest.

Covers:
- feat~chat-pane-control~1

Needs: dsn

### Choose the message layout
`req~choose-message-layout~1`

The application sets one of three layouts, and may change it at any time; the same messages are shown again in the new layout:

- **Bubbles** — talk bubbles, the local user's messages on the right, everyone else's on the left, the sender named above the first bubble of a group.
- **IRC** — one line per message: time, `<sender>`, text.
- **Modern** — left-aligned, a header with sender and time above each group of consecutive messages.

Consecutive messages of one sender within a few minutes form a group.

Covers:
- feat~selectable-message-layouts~1

Needs: dsn

### Follow the theme
`req~follow-theme~1`

Dropped into any application, the pane looks native without setup: every color comes from the JavaFX theme's standard variables, so it is readable in light and dark themes with no code change.
The pane brings no theming layer of its own; an application restyles it with ordinary CSS on documented style classes and pseudo-classes, following JavaFX conventions.

Covers:
- feat~theme-aware-styling~1

Needs: dsn

### Localizable texts
`req~localizable-texts~1`

The few texts the pane shows itself (the context menu's *Copy* and *Select All*) appear in the application's language: the application hands the pane its translation.

Covers:
- feat~chat-pane-control~1

Needs: dsn

### Named module
`req~named-module~1`

The library ships a `module-info` declaring `org.jabref.chatpane`, exporting only its API packages and requiring JavaFX transitively, so a modular application needs a single `requires org.jabref.chatpane`.

Covers:
- feat~java-module-system~1

Needs: dsn

## Design

### Message model
`dsn~chat-message-model~3`

`ChatMessage` is an immutable record `(sender, text, sentAt, direction, status)`; all parts are required, a four-part constructor means `Status.SENT`, and `withText`/`withStatus` make changed copies (what a live update replaces a message with).
`Status` is `SENT`, `PENDING` or `ERROR`, with `cssName()` like `Direction`.
`Direction` is `INCOMING` or `OUTGOING` (the local user's messages) — the pane does not know user identities, the application does; an enum rather than a flag, so a call site reads as what it means, and `cssName()` gives the same word the CSS hooks use (`:incoming`/`:outgoing`, style names `incoming`/`outgoing`).

Covers:
- req~show-conversation~2
- req~message-status~1
- req~live-update~1

Needs: impl, utest

### Control
`dsn~chat-pane-control~5`

`ChatPane` extends `Control` and follows the `javafx.scene.control` architecture (MADR 0009): the control holds state only, its default skin (`createDefaultSkin`) renders it, and its user-agent stylesheet styles it.
State: an `ObservableList<ChatMessage>` of messages, a styleable `ObjectProperty<MessageLayout>` (default `MODERN`; `null` reads as the default), settable from CSS as `-cp-message-layout` through `getClassCssMetaData()`/`getControlCssMetaData()`, and an `ObjectProperty<DateTimeFormatter>` for the time of a message, used by every layout alike (default: the locale's short time in the system zone; `null` reads as the default, a formatter without zone gets the system zone), a `MessageRenderer` property (default plain text, `null` reads as the default) a nullable `Consumer<String>` link handler, an `ObservableList<MessageAction>` of message actions, and a `UnaryOperator<String>` text localizer (English text in, shown text out; default identity, `null` reads as the default) for the pane's own texts `TEXT_COPY` and `TEXT_SELECT_ALL`; a change of the formatter or the renderer re-renders the shown view.
The active layout is exposed to CSS as a pseudo-class on the pane (`:bubbles`, `:irc`, `:modern`), kept by one method from an `EnumMap`; the style class is `chat-pane`.

Covers:
- req~show-conversation~2
- req~choose-message-layout~1
- req~rich-text~1
- req~message-actions~1
- req~localizable-texts~1

Needs: impl, utest

### Bubble body
`dsn~bubble-text~1`

In the bubble layout, a message body is a `BubbleText`: a read-only, non-focus-traversable `RichTextArea` (style classes `rich-text-area` and `message-body`) over a one-message `TranscriptModel` in the `BodyFormat` — the format the modern layout uses for its bodies — so selection, the context menu, *Copy*, Markdown styles and links behave as in the transcript.
`chatpane.css` flattens it (no background, border, padding or caret, wrapping, `-fx-use-content-height`), so it is as tall as its text.
Its preferred width is its widest rendered line, measured with `Text` nodes that carry the spans' style names inside the bubble, where the stylesheet gives them the fonts the area's text gets (Workaround W5); the bubble's max width (70 % of the list) caps it, and the area wraps below that.
Selection colors contrast with the bubble around it: the accent color in an incoming bubble, the laddered text color in an outgoing one, whose background already is the accent — both half transparent.

Covers:
- req~standard-text-actions~1
- req~show-conversation~2
- req~rich-text~1

Needs: impl, utest

### Message renderers
`dsn~message-renderers~1`

`MessageRenderer` (public, functional) turns a text into `TextLine`s — a `Kind` (`PARAGRAPH`, `HEADING`, `QUOTE`, `LIST_ITEM`, `CODE_BLOCK`), a level (heading level, nesting depth), whether it starts a block, and `TextSpan`s (text, `Style`s `BOLD`/`ITALIC`/`CODE`/`STRIKETHROUGH`, an optional link target); `lineCount` defaults to `render(text).size()`.
`plainText()` is one plain line per line of the text, with a counting `lineCount`.
`markdown()` is commonmark-java with the GFM strikethrough extension (MADR 0011), walked by a visitor after JabRef's `MarkdownTextFlow`: `•` bullets and numbered markers as the first span of an item, nesting as the level, fenced and indented code as `CODE_BLOCK` lines, HTML as literal text, images as `[alt]` linked to their source; a single line break stays a line break.
Both views render through a `RenderContext` that reads the pane's time formatter, renderer and link handler fresh each time.

Covers:
- req~rich-text~1

Needs: impl, utest

### Links
`dsn~message-links~1`

Built paragraphs travel as `TranscriptLine`s: the `RichParagraph` plus the character ranges of its links (after the IRC prefix, where there is one); `TranscriptModel.linkAt(TextPos)` finds the target under a position.
`LinkInteraction` installs on the transcript and on every bubble body alike: a hand cursor over a link while the pane has a link handler (set on the area's `.content` node, whose theme cursor would win otherwise), and a primary single click that moved nothing and selected nothing hands the target to the handler.

Covers:
- req~rich-text~1

Needs: impl, utest

### Display only
`dsn~no-input-in-the-pane~1`

`ChatPane` has no input node and no send API: its skin holds only the views of the messages.
The demo builds its input line (`TextField` + *Send*) next to the pane and appends to `getMessages()`.

Covers:
- req~display-only~1

Needs: impl

### Layouts
`dsn~message-layouts~2`

`MessageLayout` is an enum `BUBBLES`, `IRC`, `MODERN`, named after Element's layout switch; `cssName()` gives the lower-case name the CSS uses (pseudo-class, `-cp-message-layout` value), like `ChatMessage.Direction.cssName()`.

Covers:
- req~choose-message-layout~1

Needs: impl

### Skin
`dsn~chat-pane-skin~7`

`ChatPaneSkin` holds one `ConversationView` per layout in an `EnumMap` — the one place that maps layouts to views: `BUBBLES` → `BubbleView` (`dsn~bubble-view~2`), `IRC` → `TranscriptView` with `IrcTranscript`, `MODERN` → `TranscriptView` with `ModernTranscript` (`dsn~transcript-view~5`).
A layout change hides the shown view, puts the new one on screen and then shows it (so its text cells are built in the scene, with the pane's CSS); a change of the time formatter, the renderer or the actions renders the shown view again; the skin's only other job is to report each change of the messages to the shown view, as an append, the replacement of one message, or anything else (`dsn~message-changes~2`).
Listeners go through `SkinBase.registerChangeListener`/`registerListChangeListener`, so `dispose()` removes them; it also hides the shown view, so a replaced skin keeps nothing alive.
The skin is `final` (Effective Java item 19): it offers no hooks to override, and a different look is a different skin (`setSkin`, `-fx-skin`).

Covers:
- req~show-conversation~2
- req~choose-message-layout~1

Needs: impl, utest

### Conversation views
`dsn~conversation-views~3`

A `ConversationView` is one way of showing the conversation: `node()`, `show(messages)`, `hide()`, `appended(messages, from)`, `updated(messages, index)` (one message replaced — updated in place, or re-rendered if the view cannot), `replaced(messages)`, `restyle()` (the styles the text is drawn with changed: draw it again, `dsn~transcript-restyle~1`).
Only the shown view tracks the messages; `show` builds it from the current list, `hide` releases them.
Every view follows the newest message by the same rule: after `show`, `appended`, `replaced`, or `updated` of the last message, it scrolls to the end unless the user has text selected in it.

Covers:
- req~show-conversation~2
- req~select-across-messages~1
- req~live-update~1

Needs: impl, utest

### Change classification
`dsn~message-changes~2`

`MessageChanges.appendedFrom` turns a list change into the index of the first appended message when the change only added messages at the end; `replacedAt` into the index of the one message a `set(index, …)` replaced; both return `NOT_AN_APPEND` for anything else, and reset the change afterwards, so later listeners read it whole.

Covers:
- req~show-conversation~2
- req~live-update~1

Needs: impl, utest

### Bubble view
`dsn~bubble-view~2`

`BubbleView` shows a virtualized `ListView` (style class `chat-pane-list`, MADR 0009) of `MessageCell`s over its own copy of the messages: `show`/`replaced` set it, `appended` adds the new tail, `updated` sets the one item (and its successor's, if the change affects that one's grouping), `hide` clears it.
Text counts as selected when the scene's focus owner is a `BubbleText` of this list with a non-empty selection; otherwise a change scrolls to the last message — twice, the second time on the next pulse, since a new body knows its final height only after its area laid out.
The wheel over a body scrolls the list by itself (the content-high area passes wheel events on).

Covers:
- req~show-conversation~2
- req~standard-text-actions~1

Needs: impl, utest

### Bubble cells
`dsn~message-cell-bubbles~3`

`MessageCell` builds the row of the `BUBBLES` layout: a bubble (`.message-bubble`, at most 70 % of the list width, aligned right for outgoing) with the sender, the `BubbleText` body (plus its measuring node) and the time.
The cell carries the pseudo-classes `:outgoing` or `:incoming`, and `:sent`, `:pending` or `:error`; a message continuing its group (`dsn~message-grouping~1`) also carries `:continued` and drops the sender.
The time label's tooltip gives date and time in full (the locale's medium format, the pane's zone), as JabRef's chat has it.
The cell's preferred width is zero so text wraps instead of widening the list.

Covers:
- req~show-conversation~2
- req~choose-message-layout~1
- req~message-status~1

Needs: impl, utest

### Message actions (design)
`dsn~message-actions~2`

A `MessageAction` (public record) has a text, an optional graphic supplier (a fresh node per use), an `appliesTo` predicate and an `onAction` consumer that gets the message instance.
`MessageMenu` replaces the areas' default context menu — in the transcript and in every bubble body alike — with *Copy* (disabled without a selection), *Select All*, and, after a separator, the pane's actions that apply to the message under the pointer (the transcript finds it through `TranscriptModel.messageAt`).
In bubbles, the same actions are also `message-action` buttons in a `message-actions` box on the inner side of the bubble (as in JabRef's AI chat), laid out always and shown while the pointer is over the cell, so the bubble does not jump; a graphic makes the text a tooltip.
The menu texts *Copy* and *Select All* go through the pane's text localizer (`dsn~chat-pane-control~5`); action texts are the application's own.

Covers:
- req~message-actions~1
- req~standard-text-actions~1
- req~localizable-texts~1

Needs: impl, utest

### Transcript view
`dsn~transcript-view~5`

`TranscriptView` shows one read-only `RichTextArea` (style class `chat-pane-transcript`, incubator module `jfx.incubator.richtext`, MADR 0010) over a `TranscriptModel`, filled by its `TranscriptFormat` (`dsn~transcript-paragraphs~4`); the model is the area's own, no second reference is kept.
`show`/`replaced` set a new model, `appended` appends to it, `updated` updates one message in it (a new model only if that update changes the next message's grouping), `hide` sets `null`; the context menu is `MessageMenu` (`dsn~message-actions~2`).
Selection, the standard context menu and *Copy* are the area's own; copying exports plain text among the model's formats; links work through `LinkInteraction` (`dsn~message-links~1`).
Read-only, wrapping, the hidden caret and no current-paragraph highlight are set in code — in `chatpane.css` a CSS pass that sets them again breaks the area (Workaround W6); the content padding stays in CSS.
Following the newest message (`dsn~conversation-views~3`) moves the hidden caret to the end of the document, which scrolls there.

Covers:
- req~select-across-messages~1
- req~standard-text-actions~1
- req~choose-message-layout~1
- req~live-update~1

Needs: impl, utest

### Restyle on CSS changes
`dsn~transcript-restyle~1`

A `RichTextArea` resolves a segment's style names into styles once, when it builds the text cell (Workaround W7): its text would not follow a theme switch, and cells built before the pane's CSS was in place would keep the defaults.
The skin therefore holds a `StyleProbe`, an invisible, unmanaged `Label` whose `chatpane.css` background lists every lookup the text is drawn with and whose font is the text font; when its background or font changes, the shown view's `restyle()` runs — the transcript drops the area's built cells (`RichTextAreaSkin.refreshLayout()`), the bubble list rebuilds its cells.
The probe's first styling after the pane enters a scene triggers it too, which rebuilds cells built before the CSS.

Covers:
- req~follow-theme~1

Needs: impl, utest

### Transcript formats
`dsn~transcript-paragraphs~4`

A `TranscriptFormat` turns one message into built paragraphs (`TranscriptLine`: the `RichParagraph` and its links), and says how many it would build (`paragraphCount`) without building them — the transcript model counts every message but builds only the visible ones (`dsn~transcript-model~4`); the count is the renderer's `lineCount` (plus the header).
Three implementations, over the rendered `TextLine`s of the `RenderContext`: `IrcTranscript`, `ModernTranscript`, and `BodyFormat` (the lines alone — the body of a bubble and of a modern entry).
`IrcTranscript`: the first rendered line of a message prefixed `time <sender> `, every further line a paragraph of its own.
`ModernTranscript`: a header paragraph `sender  time` for a message that starts a group, then the `BodyFormat` paragraphs.
Shared building blocks are in `TranscriptSegments`: a paragraph that starts a group has 6 px space above it, one that starts a block within a message 3 px, a nested list item or quote 16 px of indentation per level (the model's unit is pixels); segments carry only CSS style names — `message-time`, `message-sender`, `message-text`, `incoming` or `outgoing`, `continued` for a message that continues its group (the states a bubble cell has as pseudo-classes), and for rendered text `line-<kind>` (plus `line-heading-<level>`) and `span-<style>` (plus `span-link`) — so colors stay in CSS (MADR 0007).

Covers:
- req~choose-message-layout~1
- req~select-across-messages~1

Needs: impl, utest

### Transcript model
`dsn~transcript-model~4`

`TranscriptModel` is a read-only `StyledTextModel` (`StyledTextModelViewOnlyBase`), virtual like the bubble list: it holds the messages and, per message, the index of its first paragraph (an `int[]`), and builds a message's paragraphs with its `TranscriptFormat` only when the area asks for one of them; the last 256 messages built stay cached, links included (`linkAt`).
Grouping (`dsn~message-grouping~1`) looks at the message before, also across an append.
Measured with 100 000 messages: holding every built paragraph cost about 107 MB, the virtual model about 6 MB — what the bubble list costs.
`update(messages, index)` replaces one message: its paragraphs are rebuilt, the offsets after it shift, and the change event covers exactly its old paragraphs (text added on the first line, one line per further paragraph, the last one's length after them); it refuses (`false`) if the replacement changes whether the next message continues the group.
`append` adds messages at the end and fires the matching content change — no characters on the old last line, one line per paragraph, the last paragraph's length after them — so the area keeps its scroll position and selection.
An empty transcript is one empty placeholder paragraph (a document is never empty), which the first append replaces, firing its text as added on the existing line.

Covers:
- req~show-conversation~2
- req~choose-message-layout~1

Needs: impl, utest

### Grouping
`dsn~message-grouping~1`

A message continues the previous one's group when both have the same sender and direction and it was sent no earlier than, and at most five minutes after, the previous one.
A message listed after a newer one starts a new group.

Covers:
- req~choose-message-layout~1

Needs: impl, utest

### Stylesheet
`dsn~chatpane-stylesheet~5`

`chatpane.css` is the control's user-agent stylesheet.
It holds layout and structure and no palette of its own (MADR 0007): colors come only from the standard Modena lookups — a container sets `-fx-background` (`-fx-control-inner-background` for rows, `-fx-base` for a bubble, `-fx-accent` for an outgoing bubble) and its labels take `-fx-text-background-color`, which Modena ladders against it; senders use `-fx-accent`, times the text color at 70 % opacity (Modena's `-fx-mid-text-color` is a fixed dark gray, unreadable on dark).
Rendered text takes `-fx-fill` (the area resolves style names into styles; Workaround W3): in the transcript text and times `-fx-text-inner-color`, senders `-fx-accent`; in bubbles the laddered `-fx-text-background-color`.
Span and line styles are the same everywhere: bold, italic, monospace code, strikethrough, heading sizes 1.4/1.2/1.1 em, quotes italic and dimmed, links in the accent and underlined — on an outgoing bubble (the accent) in the text color.
The bubble body is flattened (`dsn~bubble-text~1`) and its selection highlight set per bubble kind.
The style probe's background lists the lookups the text uses (`dsn~transcript-restyle~1`); the areas' wrapping and caret settings are no longer set here (Workaround W6).
Status without a color of its own (Modena has no error lookup): a pending bubble dimmed, a failed one with a dashed outline in the text color, pending and failed text italic in both views; action buttons small.
Sizes are in `em`.
The list has no visible selection and no zebra rows: a chat row is not a pickable item.
The CSS hooks (style classes for structure, pseudo-classes for state) are documented in the README's *CSS reference*.

Covers:
- req~follow-theme~1

Needs: impl

### Module descriptor
`dsn~module-descriptor~3`

`module org.jabref.chatpane` — `requires transitive javafx.controls` (the API exposes JavaFX types), `requires jfx.incubator.richtext` (the transcript, not transitive: no incubator type in the API; MADR 0010), `requires org.commonmark` and `org.commonmark.ext.gfm.strikethrough` (Markdown, not transitive; MADR 0011), `requires static transitive org.jspecify` (annotations only, but part of the public API), `requires org.slf4j` (MADR 0005); exports `org.jabref.chatpane` and `org.jabref.chatpane.skin`.

Covers:
- req~named-module~1

Needs: impl
