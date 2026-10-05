package org.jabref.chatpane;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/// A run of text in one style within a [TextLine]: what a [MessageRenderer] produces.
///
/// @param text   the characters, never containing a line break
/// @param styles inline styles; each becomes a CSS style name `span-<name>` on the rendered text
/// @param link   the target if the span is a link, handed to [ChatPane#linkHandlerProperty()] on click
/// @param token  what the span is in a highlighted code block ([CodeToken#type()]); it becomes a CSS
///               style name `token-<token>` on the rendered text. `null` outside highlighted code.
public record TextSpan(String text, Set<Style> styles, @Nullable String link, @Nullable String token) {

    private static final Pattern CSS_NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

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
        if (token != null && !isCssName(token)) {
            throw new IllegalArgumentException("not a CSS name (lower-case letters, digits, '-'): " + token);
        }
    }

    /// A span outside highlighted code.
    public TextSpan(String text, Set<Style> styles, @Nullable String link) {
        this(text, styles, link, null);
    }

    /// Lower-case letters and digits, in words joined by `-`.
    static boolean isCssName(String name) {
        return CSS_NAME.matcher(name).matches();
    }

    /// A span without style or link.
    public static TextSpan plain(String text) {
        return new TextSpan(text, Set.of(), null);
    }
}
