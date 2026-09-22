# Demo application

## Requirements

### Try the layouts
`req~demo-layouts~1`

The demo opens a window with a sample conversation between several senders, including the local user, one message long enough to wrap, and a group of consecutive messages.
One toggle per layout switches the pane; an input line appends the typed text as the local user's message.

Covers:
- feat~demo-application~1

Needs: dsn

### An answering assistant
`req~demo-assistant~1`

Every sent message gets an answer from a pretend assistant, as in an AI chat: it appears pending, grows word by word in Markdown, and ends sent — or failed, when the question contains "fail", to be answered again with *Retry*; every message can be deleted.

Covers:
- feat~demo-application~1

Needs: dsn

### Light and dark
`req~demo-theme~1`

A toggle at the top right switches the demo between a light theme, a dark theme, and following the operating system's setting (the default), so the pane can be checked in both.

Covers:
- feat~demo-application~1
- feat~theme-aware-styling~1

Needs: dsn

### Runs on the module path
`req~demo-module-path~1`

The demo is itself a named module and runs on the module path, so it proves the library works there, with logging routed to a real backend.

Covers:
- feat~demo-application~1
- feat~java-module-system~1

Needs: dsn

## Design

### Demo app
`dsn~demo-app~3`

`DemoApp` (module `org.jabref.chatpane.demo`) is a `BorderPane`: on top the layout toggles `layout-<name>` on the left and the theme toggles `theme-<name>` on the right (both from one `toggles` helper), the `ChatPane` in the center, `message-input` and *Send* at the bottom.
A third toggle bar switches the text format (plain, Markdown; the default is Markdown), and links open through `HostServices`.
`--layout=<name>`, `--theme=<name>` and `--text=<name>` pick the initial ones.
`DemoResponder` answers each sent message: a `PENDING` message that a `Timeline` replaces word by word (the pane's live update), then `SENT`, or `ERROR` if the question contains "fail"; the pane's message actions are *Delete* (any message, found by identity) and *Retry* (the assistant's failed answers).
`gradlew :demo:run` starts it with `mainModule` set, so Gradle puts the modules on the module path; SLF4J reaches tinylog through service binding (`slf4j-tinylog`, `tinylog-impl` as runtime-only dependencies, MADR 0005).

Covers:
- req~demo-layouts~1
- req~demo-module-path~1
- req~demo-assistant~1

Needs: impl

### Theme switch
`dsn~demo-theme-switch~1`

`DemoTheme` is `LIGHT`, `DARK` or `SYSTEM`; applying one sets the scene's color-scheme preference (`Scene.getPreferences().setColorScheme`, `null` for `SYSTEM` = follow the platform), which also switches the window decorations.
A subscription on the scene's effective color scheme adds `dark.css` — Modena's own variables darkened, the demo's theme, not the library's — when it is dark and removes it otherwise.

Covers:
- req~demo-theme~1

Needs: impl
