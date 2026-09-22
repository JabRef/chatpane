# Features

Vision-level features, distilled from the [README](../../README.md).

## Chat pane control
`feat~chat-pane-control~1`

A JavaFX application embeds a single control that shows a chat conversation — sender, time and text of each message — the way messengers such as Element/Matrix or WhatsApp do.

Needs: req

## Selectable message layouts
`feat~selectable-message-layouts~1`

The application (and through it the user) decides how the messages are displayed: as talk bubbles left and right, as classic IRC-style lines, or message by message in the modern style — and can switch at any time without losing the conversation.

Needs: req

## Rich message text
`feat~rich-message-text~1`

Message texts can be rendered — Markdown out of the box, or by the application's own renderer — with clickable links, alike in every layout.

Needs: req

## Theme-aware styling
`feat~theme-aware-styling~1`

The pane follows the application's JavaFX theme (Modena light or dark, AtlantaFX, …) without per-theme code, and an application can restyle it from CSS alone.

Needs: req

## Java Module System
`feat~java-module-system~1`

The library is a named Java module that works on the module path as well as on the class path.

Needs: req

## Demo application
`feat~demo-application~1`

A small runnable demo shows the pane with a sample conversation and lets a developer try every layout.

Needs: req
