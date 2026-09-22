package org.jabref.chatpane.internal;

import java.util.Arrays;
import java.util.List;

import org.jabref.chatpane.MessageRenderer;
import org.jabref.chatpane.TextLine;

/// [MessageRenderer#plainText()]: one plain line per line of the text.
// [impl->dsn~message-renderers~1]
public final class PlainTextRenderer implements MessageRenderer {

    public static final PlainTextRenderer INSTANCE = new PlainTextRenderer();

    private PlainTextRenderer() {
    }

    @Override
    public List<TextLine> render(String text) {
        return Arrays.stream(text.split("\\R", -1)).map(TextLine::plain).toList();
    }

    /// One more than the line breaks `\R` matches, without splitting: `\r\n` counts once, and so
    /// do `\n`, `\r`, `\u000B`, `\f`, `\u0085`, ` `, ` `.
    @Override
    public int lineCount(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '\r' -> {
                    lines++;
                    if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                }
                case '\n', '\u000B', '\f', '\u0085', ' ', ' ' -> lines++;
                default -> {
                }
            }
        }
        return lines;
    }
}
