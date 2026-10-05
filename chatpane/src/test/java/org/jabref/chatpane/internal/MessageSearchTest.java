package org.jabref.chatpane.internal;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.FindMatch;
import org.jabref.chatpane.MessageRenderer;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.assertj.core.api.Assertions.assertThat;

// [utest->dsn~find-in-messages~1]
class MessageSearchTest {

    private static List<ChatMessage> messages(String... texts) {
        return Arrays.stream(texts).map(text -> new ChatMessage("bob", text, Instant.EPOCH, INCOMING)).toList();
    }

    @Test
    void findsEveryOccurrenceIgnoringCaseInReadingOrder() {
        List<FindMatch> matches = MessageSearch.find(messages("Cocoa and coconut", "none", "x\nCOCO"),
                MessageRenderer.plainText(), "co");
        assertThat(matches).containsExactly(
                new FindMatch(0, 0, 0, 2),
                new FindMatch(0, 0, 2, 4),
                new FindMatch(0, 0, 10, 12),
                new FindMatch(0, 0, 12, 14),
                new FindMatch(2, 1, 0, 2),
                new FindMatch(2, 1, 2, 4));
    }

    @Test
    void matchesDoNotOverlap() {
        assertThat(MessageSearch.find(messages("aaa"), MessageRenderer.plainText(), "aa"))
                .containsExactly(new FindMatch(0, 0, 0, 2));
    }

    @Test
    void emptyQueryFindsNothing() {
        assertThat(MessageSearch.find(messages("anything"), MessageRenderer.plainText(), "")).isEmpty();
    }

    @Test
    void queryIsLiteral() {
        assertThat(MessageSearch.find(messages("a.b axb"), MessageRenderer.plainText(), "a.b"))
                .containsExactly(new FindMatch(0, 0, 0, 3));
    }

    /// Positions are the rendered text's: the Markdown syntax is gone, the bullet is there.
    @Test
    void searchesTheRenderedText() {
        List<FindMatch> matches = MessageSearch.find(messages("**bold** word\n\n- word"), MessageRenderer.markdown(), "word");
        assertThat(matches).containsExactly(new FindMatch(0, 0, 5, 9), new FindMatch(0, 1, 2, 6));
        assertThat(MessageSearch.find(messages("**bold**"), MessageRenderer.markdown(), "**")).isEmpty();
    }

    /// Lower-casing "İ" gives two characters; positions must still be the text's own.
    @Test
    void positionsSurviveCaseFoldingThatChangesLength() {
        assertThat(MessageSearch.find(messages("İİ x"), MessageRenderer.plainText(), "x"))
                .containsExactly(new FindMatch(0, 0, 3, 4));
    }
}
