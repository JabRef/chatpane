package org.jabref.chatpane.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.chatpane.CodeHighlighter;
import org.jabref.chatpane.CodeToken;

/// The demo's [CodeHighlighter]: ` ```json ` blocks split into `property` (an object key),
/// `string`, `number`, `keyword` (`true`, `false`, `null`) and `punctuation`; other languages stay
/// plain. A lexer, not a parser — invalid JSON is highlighted as far as it goes, which is what an
/// answer still being generated needs. The colors are the demo's (`demo.css`, `dark.css`).
// [impl->dsn~demo-code-highlighting~1]
final class JsonHighlighter implements CodeHighlighter {

    private static final Pattern TOKEN = Pattern.compile(
            "(?<string>\"(?:[^\"\\\\\\n]|\\\\.)*\"?)(?<key>\\s*:)?"
                    + "|(?<number>-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"
                    + "|(?<keyword>\\b(?:true|false|null)\\b)"
                    + "|(?<punctuation>[{}\\[\\],:])");

    @Override
    public List<CodeToken> highlight(String language, String code) {
        if (!language.equalsIgnoreCase("json")) {
            return List.of(CodeToken.plain(code));
        }
        List<CodeToken> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(code);
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() > end) {
                tokens.add(CodeToken.plain(code.substring(end, matcher.start())));
            }
            if (matcher.group("string") != null) {
                // A key is the string before a colon; the colon is punctuation of its own.
                tokens.add(new CodeToken(matcher.group("string"), matcher.group("key") != null ? "property" : "string"));
                if (matcher.group("key") != null) {
                    String space = matcher.group("key").substring(0, matcher.group("key").length() - 1);
                    if (!space.isEmpty()) {
                        tokens.add(CodeToken.plain(space));
                    }
                    tokens.add(new CodeToken(":", "punctuation"));
                }
            } else if (matcher.group("number") != null) {
                tokens.add(new CodeToken(matcher.group(), "number"));
            } else if (matcher.group("keyword") != null) {
                tokens.add(new CodeToken(matcher.group(), "keyword"));
            } else {
                tokens.add(new CodeToken(matcher.group(), "punctuation"));
            }
            end = matcher.end();
        }
        if (end < code.length()) {
            tokens.add(CodeToken.plain(code.substring(end)));
        }
        return tokens;
    }
}
