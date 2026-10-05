package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.FindMatch;

/// The pane's find matches ([org.jabref.chatpane.ChatPane#getFindMatches()]) by message, for the
/// formats that build a message's paragraphs: they know the message, not its index.
///
/// Keyed by identity — two equal messages in the list are two messages. Should the same instance
/// be listed twice, both show the matches of the first, which are the same text's.
final class FindHighlights {

    /// The style name of every match's highlight.
    static final String MATCH = "find-match";

    /// The style name added to the current match's highlight.
    static final String CURRENT = "find-current";

    /// A match within one rendered line: characters `[start, end)`, and whether it is the current one.
    record Range(int start, int end, boolean current) {

        /// The highlight's style names.
        String[] styleNames() {
            return current ? new String[] {MATCH, CURRENT} : new String[] {MATCH};
        }
    }

    private final Map<ChatMessage, List<FindMatch>> byMessage = new IdentityHashMap<>();
    private @Nullable FindMatch current;

    /// Takes the pane's current state: `matches` index into `messages`.
    // [impl->dsn~find-highlights~1]
    void update(List<ChatMessage> messages, List<FindMatch> matches, @Nullable FindMatch current) {
        byMessage.clear();
        this.current = current;
        int owner = -1;
        for (FindMatch match : matches) {
            ChatMessage message = messages.get(match.message());
            List<FindMatch> ofMessage = byMessage.get(message);
            if (ofMessage == null) {
                ofMessage = new ArrayList<>();
                byMessage.put(message, ofMessage);
                owner = match.message();
            }
            if (match.message() == owner) {
                ofMessage.add(match);
            }
        }
    }

    /// The matches in line `line` of `message`, in order.
    List<Range> in(ChatMessage message, int line) {
        List<FindMatch> ofMessage = byMessage.get(message);
        if (ofMessage == null) {
            return List.of();
        }
        List<Range> ranges = new ArrayList<>();
        for (FindMatch match : ofMessage) {
            if (match.line() == line) {
                ranges.add(new Range(match.start(), match.end(), isCurrent(match)));
            }
        }
        return ranges;
    }

    private boolean isCurrent(FindMatch match) {
        return current != null && current.line() == match.line() && current.start() == match.start()
                && current.message() == match.message();
    }
}
