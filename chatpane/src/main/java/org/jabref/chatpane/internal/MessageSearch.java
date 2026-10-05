package org.jabref.chatpane.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.FindMatch;
import org.jabref.chatpane.MessageRenderer;
import org.jabref.chatpane.TextLine;

/// Finds a query in the rendered text of messages, for [org.jabref.chatpane.ChatPane]'s find.
///
/// The query is literal and case-insensitive (Unicode case folding); matches do not overlap. A
/// regular expression rather than lower-casing both sides: lower-casing can change a text's length
/// (`İ` becomes two characters), and the match positions must be the shown text's.
// [impl->dsn~find-in-messages~1]
public final class MessageSearch {

    private MessageSearch() {
    }

    /// Every occurrence of `query` in `messages` as `renderer` renders them, in reading order;
    /// none for an empty query.
    public static List<FindMatch> find(List<ChatMessage> messages, MessageRenderer renderer, String query) {
        if (query.isEmpty()) {
            return List.of();
        }
        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        List<FindMatch> matches = new ArrayList<>();
        for (int message = 0; message < messages.size(); message++) {
            List<TextLine> lines = renderer.render(messages.get(message).text());
            for (int line = 0; line < lines.size(); line++) {
                Matcher matcher = pattern.matcher(lines.get(line).plainText());
                while (matcher.find()) {
                    matches.add(new FindMatch(message, line, matcher.start(), matcher.end()));
                }
            }
        }
        return matches;
    }
}
