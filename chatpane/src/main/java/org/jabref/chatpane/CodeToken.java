package org.jabref.chatpane;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/// A run of code with one meaning, as a [CodeHighlighter] splits a code block: a keyword, a
/// string, a number, …
///
/// @param text the characters; may contain line breaks, the renderer splits the block into lines
/// @param type what the run is, as a CSS name (lower-case letters, digits, `-`), e.g. `string`;
///             the rendered text carries it as the style name `token-<type>`. `null` for plain
///             code without a style of its own.
public record CodeToken(String text, @Nullable String type) {

    public CodeToken {
        Objects.requireNonNull(text, "text");
        if (type != null && !TextSpan.isCssName(type)) {
            throw new IllegalArgumentException("not a CSS name (lower-case letters, digits, '-'): " + type);
        }
    }

    /// Code without a style of its own.
    public static CodeToken plain(String text) {
        return new CodeToken(text, null);
    }
}
