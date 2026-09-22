package org.jabref.chatpane;

import java.util.List;

import org.jabref.chatpane.internal.MarkdownRenderer;
import org.jabref.chatpane.internal.PlainTextRenderer;

/// Turns a message text into the lines every layout shows — the hook for how message bodies read
/// ([ChatPane#messageRendererProperty()]).
///
/// Two come with the library: [#plainText()], the default, and [#markdown()]. An application can
/// plug in its own, e.g. one on the Markdown parser it already uses; the output is this library's
/// own small model ([TextLine], [TextSpan]), styled by CSS names, never by colors.
///
/// A renderer is called on the JavaFX thread, for the messages on screen and whenever the
/// conversation is re-rendered; it should be fast and must be free of side effects.
@FunctionalInterface
public interface MessageRenderer {

    /// The lines of `text`, in order; at least one (an empty text is one empty line).
    List<TextLine> render(String text);

    /// `render(text).size()`, for which the transcript asks about every message but builds only
    /// the ones on screen; override when it can be told without rendering.
    default int lineCount(String text) {
        return render(text).size();
    }

    /// The text as it is: one plain line per line of the text.
    static MessageRenderer plainText() {
        return PlainTextRenderer.INSTANCE;
    }

    /// CommonMark with strikethrough (`~~…~~`): headings, emphasis, inline and block code, links,
    /// bullet and numbered lists, block quotes; HTML is shown as text, not interpreted. A single
    /// line break stays a line break, as chat users expect.
    static MessageRenderer markdown() {
        return MarkdownRenderer.INSTANCE;
    }
}
