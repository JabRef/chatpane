package org.jabref.chatpane;

/// One occurrence of the pane's find query ([ChatPane#findQueryProperty()]) in a message text, as
/// rendered: positions refer to the [TextLine]s the pane's [MessageRenderer] makes, so a match
/// never spans two lines and never includes Markdown syntax that is not shown.
///
/// @param message index of the message in [ChatPane#getMessages()]
/// @param line    index of the rendered line in the message
/// @param start   first character of the match in the line's [TextLine#plainText()]
/// @param end     the character after the match
public record FindMatch(int message, int line, int start, int end) {

    public FindMatch {
        if (message < 0 || line < 0 || start < 0 || end <= start) {
            throw new IllegalArgumentException("not a match: " + message + "/" + line + " [" + start + ", " + end + ")");
        }
    }
}
