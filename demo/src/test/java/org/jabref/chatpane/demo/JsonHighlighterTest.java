package org.jabref.chatpane.demo;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.CodeToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

// [utest->dsn~demo-code-highlighting~1]
class JsonHighlighterTest {

    private static final JsonHighlighter JSON = new JsonHighlighter();

    private static String joined(List<CodeToken> tokens) {
        return tokens.stream().map(CodeToken::text).collect(Collectors.joining());
    }

    @Test
    void namesEveryKindOfToken() {
        String code = "{\"a\" : \"x\\\"y\", \"n\": [-1.5e3, true, null]}";
        List<CodeToken> tokens = JSON.highlight("json", code);
        assertThat(tokens).extracting(CodeToken::text, CodeToken::type).containsExactly(
                tuple("{", "punctuation"),
                tuple("\"a\"", "property"), tuple(" ", null), tuple(":", "punctuation"), tuple(" ", null),
                tuple("\"x\\\"y\"", "string"), tuple(",", "punctuation"), tuple(" ", null),
                tuple("\"n\"", "property"), tuple(":", "punctuation"), tuple(" ", null),
                tuple("[", "punctuation"), tuple("-1.5e3", "number"), tuple(",", "punctuation"), tuple(" ", null),
                tuple("true", "keyword"), tuple(",", "punctuation"), tuple(" ", null),
                tuple("null", "keyword"), tuple("]", "punctuation"), tuple("}", "punctuation"));
        assertThat(joined(tokens)).isEqualTo(code);
    }

    /// An answer being generated stops anywhere; every character must still come back.
    @Test
    void unfinishedJsonAddsUp() {
        for (String code : List.of("{\"a\": \"unterminated", "{\n  \"a\": 1,\n  \"b\"", "not json at all", "")) {
            assertThat(joined(JSON.highlight("json", code))).as(code).isEqualTo(code);
        }
    }

    @Test
    void otherLanguagesStayPlain() {
        assertThat(JSON.highlight("java", "true")).containsExactly(CodeToken.plain("true"));
        assertThat(JSON.highlight("JSON", "true")).extracting(CodeToken::type).containsExactly("keyword");
    }
}
