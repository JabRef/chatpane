package org.jabref.chatpane;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/// A run of text in one style within a [TextLine]: what a [MessageRenderer] produces.
///
/// @param text   the characters, never containing a line break
/// @param styles inline styles; each becomes a CSS style name `span-<name>` on the rendered text
/// @param link   the target if the span is a link, handed to [ChatPane#linkHandlerProperty()] on click
public record TextSpan(String text, Set<Style> styles, @Nullable String link) {

    /// Inline styles a renderer can ask for.
    public enum Style {
        BOLD, ITALIC, CODE, STRIKETHROUGH;

        /// The CSS name, e.g. `bold`; the rendered text carries it as `span-bold`.
        public String cssName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public TextSpan {
        Objects.requireNonNull(text, "text");
        styles = Set.copyOf(styles);
    }

    /// A span without style or link.
    public static TextSpan plain(String text) {
        return new TextSpan(text, Set.of(), null);
    }
}
