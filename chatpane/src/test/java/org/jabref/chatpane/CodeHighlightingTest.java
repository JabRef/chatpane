package org.jabref.chatpane;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.TextLine.Kind;
import org.jabref.chatpane.TextSpan.Style;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;

// [utest->dsn~code-highlighting~1]
class CodeHighlightingTest {

    /// Marks every `true` as a keyword, the rest as plain code; records what it was asked.
    private static final class Keywords implements CodeHighlighter {

        private final List<String> languages = new ArrayList<>();

        @Override
        public List<CodeToken> highlight(String language, String code) {
            languages.add(language);
            List<CodeToken> tokens = new ArrayList<>();
            String[] parts = code.split("true", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    tokens.add(new CodeToken("true", "keyword"));
                }
                tokens.add(CodeToken.plain(parts[i]));
            }
            return tokens;
        }
    }

    private static List<TextSpan> spans(List<TextLine> lines) {
        return lines.stream().flatMap(line -> line.spans().stream()).toList();
    }

    @Test
    void tokensBecomeCodeSpansWithTheirType() {
        Keywords keywords = new Keywords();
        List<TextLine> lines = MessageRenderer.markdown(keywords).render("""
                ```json {"x": 1}
                {"a": true,
                 "b": [true]}
                ```""");
        assertThat(keywords.languages).as("the info string's first word").containsExactly("json");
        assertThat(lines).extracting(TextLine::kind, TextLine::plainText).containsExactly(
                tuple(Kind.CODE_BLOCK, "{\"a\": true,"),
                tuple(Kind.CODE_BLOCK, " \"b\": [true]}"));
        assertThat(lines.getFirst().spans()).extracting(TextSpan::text, TextSpan::token).containsExactly(
                tuple("{\"a\": ", null), tuple("true", "keyword"), tuple(",", null));
        assertThat(spans(lines)).allSatisfy(span -> assertThat(span.styles()).containsExactly(Style.CODE));
    }

    @Test
    void aTokenSpanningLinesIsSplitAtTheBreaks() {
        CodeHighlighter oneComment = (_, code) -> List.of(new CodeToken(code, "comment"));
        List<TextLine> lines = MessageRenderer.markdown(oneComment).render("```\n/* a\n\nb */\n```");
        assertThat(lines).extracting(TextLine::plainText).containsExactly("/* a", "", "b */");
        assertThat(lines.get(1).spans()).as("an empty line has no spans").isEmpty();
        assertThat(spans(lines)).extracting(TextSpan::token).containsOnly("comment");
    }

    @Test
    void indentedAndBareFencedBlocksHaveNoLanguage() {
        Keywords keywords = new Keywords();
        MessageRenderer.markdown(keywords).render("    indented\n\n```\nfenced\n```");
        assertThat(keywords.languages).containsExactly("", "");
    }

    @Test
    void inlineCodeIsNotHighlighted() {
        Keywords keywords = new Keywords();
        List<TextLine> lines = MessageRenderer.markdown(keywords).render("say `true`");
        assertThat(keywords.languages).isEmpty();
        assertThat(spans(lines)).extracting(TextSpan::token).containsOnlyNulls();
    }

    @Test
    void aFailingHighlighterLeavesTheCodePlain() {
        CodeHighlighter throwing = (_, _) -> {
            throw new IllegalStateException("broken");
        };
        CodeHighlighter losing = (_, code) -> List.of(new CodeToken(code.substring(1), "x"));
        for (CodeHighlighter highlighter : List.of(throwing, losing)) {
            List<TextLine> lines = MessageRenderer.markdown(highlighter).render("```\nab\ncd\n```");
            assertThat(lines).extracting(TextLine::plainText).containsExactly("ab", "cd");
            assertThat(spans(lines)).extracting(TextSpan::token).containsOnlyNulls();
        }
    }

    @Test
    void lineCountAgreesWithoutAskingTheHighlighter() {
        Keywords keywords = new Keywords();
        MessageRenderer renderer = MessageRenderer.markdown(keywords);
        String text = "text\n\n```\ntrue\n\ntrue\n```";
        assertThat(renderer.lineCount(text)).isEqualTo(renderer.render(text).size());
        assertThat(keywords.languages).as("only render asked").hasSize(1);
    }

    @Test
    void typesAreCssNames() {
        assertThat(new CodeToken("x", "string-escape").type()).isEqualTo("string-escape");
        for (String bad : List.of("", "String", "a b", "-a", "a-", "a.b")) {
            assertThatIllegalArgumentException().as(bad).isThrownBy(() -> new CodeToken("x", bad));
            assertThatIllegalArgumentException().as(bad).isThrownBy(() -> new TextSpan("x", Set.of(), null, bad));
        }
        assertThat(new TextSpan("x", Set.of(), null).token()).isNull();
    }
}
