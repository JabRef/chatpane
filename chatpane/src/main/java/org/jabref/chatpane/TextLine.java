package org.jabref.chatpane;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/// One line of a rendered message body — a paragraph of the IRC or modern transcript, a line in
/// a bubble. What a [MessageRenderer] turns a message text into.
///
/// @param kind        what the line is; the rendered text carries it as a CSS style name `line-<name>`
/// @param level       heading level (1–6) for [Kind#HEADING], nesting depth (1 = outermost) for
///                    [Kind#LIST_ITEM] and [Kind#QUOTE], 0 otherwise; nesting indents the line
/// @param startsBlock whether the line starts a new block after an earlier one (a paragraph after a
///                    list, …); such a line gets a little space above it
/// @param spans       the text, run by run; empty for an empty line
public record TextLine(Kind kind, int level, boolean startsBlock, List<TextSpan> spans) {

    /// What a line is.
    public enum Kind {
        PARAGRAPH, HEADING, QUOTE, LIST_ITEM, CODE_BLOCK;

        /// The CSS name, e.g. `list-item`; the rendered text carries it as `line-list-item`.
        public String cssName() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }

    public TextLine {
        Objects.requireNonNull(kind, "kind");
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative: " + level);
        }
        spans = List.copyOf(spans);
    }

    /// A plain paragraph line of one unstyled span (none if `text` is empty).
    public static TextLine plain(String text) {
        return new TextLine(Kind.PARAGRAPH, 0, false, text.isEmpty() ? List.of() : List.of(TextSpan.plain(text)));
    }

    /// The text of all spans, as it reads (and copies).
    public String plainText() {
        return spans.stream().map(TextSpan::text).collect(Collectors.joining());
    }
}
