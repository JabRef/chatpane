package org.jabref.chatpane;

import java.util.Locale;

/// How a [ChatPane] lays out its messages.
///
/// The names follow Element's layout switch, which offers the same three.
// [impl->dsn~message-layouts~2]
public enum MessageLayout {

    /// Talk bubbles: the local user's messages on the right, everyone else's on the left (WhatsApp, Signal).
    BUBBLES,

    /// One line per message: time, sender, text, all left-aligned (IRC clients).
    IRC,

    /// Message by message, left-aligned: a sender header starts each group of consecutive messages (Element, Slack).
    MODERN;

    /// The CSS name of this layout — the pane's pseudo-class while it is active and the value of
    /// `-cp-message-layout`: `bubbles`, `irc` or `modern`.
    public String cssName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
