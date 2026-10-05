package org.jabref.chatpane;

import java.util.List;

/// Splits the code blocks of Markdown messages into [CodeToken]s, so that a stylesheet can color
/// them — the hook for syntax highlighting ([MessageRenderer#markdown(CodeHighlighter)]).
///
/// The library brings no highlighter and no token colors (MADR 0007): the application knows which
/// languages its messages contain and styles the tokens with its own theme, e.g.
/// `.chat-pane .token-string { -fx-fill: …; }`.
///
/// Called on the JavaFX thread whenever a message with a code block is rendered; it should be fast
/// and must be free of side effects. If it throws, or its tokens do not add up to the code, the
/// block is shown unhighlighted.
@FunctionalInterface
public interface CodeHighlighter {

    /// The tokens of `code`, in order; their texts concatenated must be `code` exactly.
    ///
    /// @param language the first word of the fence's info string as written (` ```json ` gives
    ///                 `json`), empty for a fence without one and for an indented block
    /// @param code     the block's text, lines separated by `\n`, without a final line break
    List<CodeToken> highlight(String language, String code);
}
