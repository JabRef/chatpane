package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.FindMatch;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.assertj.core.api.Assertions.assertThat;

// [utest->dsn~find-highlights~1]
class FindHighlightsTest {

    private static ChatMessage message(String text) {
        return new ChatMessage("bob", text, Instant.EPOCH, INCOMING);
    }

    @Test
    void givesTheMatchesOfOneLineAndMarksTheCurrent() {
        ChatMessage first = message("ab ab\nab");
        ChatMessage second = message("ab");
        FindMatch current = new FindMatch(0, 0, 3, 5);
        FindHighlights highlights = new FindHighlights();
        highlights.update(List.of(first, second),
                List.of(new FindMatch(0, 0, 0, 2), current, new FindMatch(0, 1, 0, 2), new FindMatch(1, 0, 0, 2)), current);

        assertThat(highlights.in(first, 0)).containsExactly(
                new FindHighlights.Range(0, 2, false), new FindHighlights.Range(3, 5, true));
        assertThat(highlights.in(first, 1)).containsExactly(new FindHighlights.Range(0, 2, false));
        assertThat(highlights.in(second, 0)).containsExactly(new FindHighlights.Range(0, 2, false));
        assertThat(highlights.in(second, 1)).isEmpty();
    }

    /// The formats know the message, not its index: an equal message elsewhere is another one.
    @Test
    void keysByIdentity() {
        ChatMessage found = message("ab");
        ChatMessage equal = message("ab");
        FindHighlights highlights = new FindHighlights();
        highlights.update(List.of(found, equal), List.of(new FindMatch(0, 0, 0, 2)), null);

        assertThat(highlights.in(found, 0)).hasSize(1);
        assertThat(highlights.in(equal, 0)).isEmpty();
    }

    /// One instance listed twice shows its matches once per occurrence, not twice in each.
    @Test
    void sameInstanceTwiceIsNotHighlightedTwice() {
        ChatMessage twice = message("ab");
        FindHighlights highlights = new FindHighlights();
        highlights.update(List.of(twice, twice), List.of(new FindMatch(0, 0, 0, 2), new FindMatch(1, 0, 0, 2)), null);

        assertThat(highlights.in(twice, 0)).containsExactly(new FindHighlights.Range(0, 2, false));
    }

    @Test
    void styleNamesMarkTheCurrentMatch() {
        assertThat(new FindHighlights.Range(0, 1, false).styleNames()).containsExactly("find-match");
        assertThat(new FindHighlights.Range(0, 1, true).styleNames()).containsExactly("find-match", "find-current");
    }
}
