package org.jabref.chatpane;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.TextLine.Kind;
import org.jabref.chatpane.TextSpan.Style;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

// [utest->dsn~message-renderers~1]
class MessageRendererTest {

    private static final MessageRenderer PLAIN = MessageRenderer.plainText();
    private static final MessageRenderer MARKDOWN = MessageRenderer.markdown();

    private static List<String> text(List<TextLine> lines) {
        return lines.stream().map(TextLine::plainText).toList();
    }

    @Test
    void plainTextIsOneLinePerLineOfTheText() {
        assertThat(text(PLAIN.render("a\nb\r\n\nc"))).containsExactly("a", "b", "", "c");
        assertThat(PLAIN.render("**not bold**").getFirst().spans().getFirst().styles()).isEmpty();
    }

    @Test
    void plainLineCountAgreesWithRenderingForEveryLineBreak() {
        for (String text : List.of("", "a", "a\nb", "a\r\nb", "a\rb", "a\n\nb", "a\n", "\r\n\r", "a\u2028b\u2029c\u0085d\u000Be\ff")) {
            assertThat(PLAIN.lineCount(text)).as("%s", text.replace("\n", "\\n").replace("\r", "\\r"))
                    .isEqualTo(PLAIN.render(text).size());
        }
    }

    @Test
    void markdownEmphasisNestsAndCodeIsVerbatim() {
        TextLine line = MARKDOWN.render("a **bold *both*** and `x*y`").getFirst();
        assertThat(line.spans()).extracting(TextSpan::text, TextSpan::styles).containsExactly(
                tuple("a ", Set.of()),
                tuple("bold ", Set.of(Style.BOLD)),
                tuple("both", Set.of(Style.BOLD, Style.ITALIC)),
                tuple(" and ", Set.of()),
                tuple("x*y", Set.of(Style.CODE)));
    }

    @Test
    void markdownLinksCarryTheirTarget() {
        TextLine line = MARKDOWN.render("see [the docs](https://example.org) and ~~not~~ this").getFirst();
        assertThat(line.spans()).filteredOn(span -> span.link() != null)
                .singleElement()
                .satisfies(span -> {
                    assertThat(span.text()).isEqualTo("the docs");
                    assertThat(span.link()).isEqualTo("https://example.org");
                });
        assertThat(line.spans()).anySatisfy(span -> assertThat(span.styles()).containsExactly(Style.STRIKETHROUGH));
    }

    @Test
    void markdownBlocksBecomeLinesOfTheirKind() {
        List<TextLine> lines = MARKDOWN.render("""
                ## Title
                text
                continues

                - one
                - two
                  1. nested

                > quoted

                ```
                code line
                ```""");
        assertThat(lines).extracting(TextLine::kind, TextLine::level, TextLine::plainText).containsExactly(
                tuple(Kind.HEADING, 2, "Title"),
                tuple(Kind.PARAGRAPH, 0, "text"),
                tuple(Kind.PARAGRAPH, 0, "continues"),
                tuple(Kind.LIST_ITEM, 1, "\u2022 one"),
                tuple(Kind.LIST_ITEM, 1, "\u2022 two"),
                tuple(Kind.LIST_ITEM, 2, "1. nested"),
                tuple(Kind.QUOTE, 1, "quoted"),
                tuple(Kind.CODE_BLOCK, 0, "code line"));
        assertThat(lines).extracting(TextLine::startsBlock)
                .containsExactly(false, true, false, true, false, false, true, true);
    }

    @Test
    void htmlIsShownAsText() {
        assertThat(text(MARKDOWN.render("a <b>tag</b>"))).containsExactly("a <b>tag</b>");
    }

    @Test
    void emptyTextIsOneEmptyLine() {
        assertThat(text(MARKDOWN.render(""))).containsExactly("");
        assertThat(text(PLAIN.render(""))).containsExactly("");
        assertThat(MARKDOWN.lineCount("a\n\nb")).isEqualTo(MARKDOWN.render("a\n\nb").size());
    }
}
