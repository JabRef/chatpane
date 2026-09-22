package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.MessageRenderer;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.ChatMessage.Direction.OUTGOING;
import static org.assertj.core.api.Assertions.assertThat;

// [utest->dsn~transcript-paragraphs~4]
class TranscriptFormatTest {

    private static final Instant T0 = Instant.parse("2026-09-22T10:00:00Z");

    private static final List<ChatMessage> CONVERSATION = List.of(
            new ChatMessage("alice", "Hi!", T0, INCOMING),
            new ChatMessage("alice", "Two lines:\nsecond", T0.plusSeconds(10), INCOMING),
            new ChatMessage("me", "Yes.", T0.plusSeconds(20), OUTGOING));

    /// Locale and zone independent, plain text, no link handler.
    static final RenderContext PLAIN = new RenderContext(_ -> "10:00", MessageRenderer::plainText, () -> null);

    private static final IrcTranscript IRC = new IrcTranscript(PLAIN);
    private static final ModernTranscript MODERN = new ModernTranscript(PLAIN);

    /// All messages of `messages` from `from` on, each grouped with the one before — what the
    /// transcript model does.
    private static List<RichParagraph> paragraphs(TranscriptFormat format, List<ChatMessage> messages, int from) {
        List<RichParagraph> paragraphs = new ArrayList<>();
        for (int i = from; i < messages.size(); i++) {
            ChatMessage previous = i > 0 ? messages.get(i - 1) : null;
            format.paragraphs(messages.get(i), MessageGrouping.continuesGroup(previous, messages.get(i)))
                    .forEach(line -> paragraphs.add(line.paragraph()));
        }
        return paragraphs;
    }

    private static List<String> plainText(List<RichParagraph> paragraphs) {
        return paragraphs.stream().map(RichParagraph::getPlainText).toList();
    }

    private static boolean startsGroup(RichParagraph paragraph) {
        StyleAttributeMap attributes = paragraph.getParagraphAttributes();
        return attributes != null
                && Double.valueOf(TranscriptSegments.GROUP_GAP).equals(attributes.get(StyleAttributeMap.SPACE_ABOVE));
    }

    @Test
    void ircIsOneLinePerMessageLineWithTimeAndSender() {
        List<RichParagraph> irc = paragraphs(IRC, CONVERSATION, 0);
        assertThat(plainText(irc)).containsExactly(
                "10:00 <alice> Hi!",
                "10:00 <alice> Two lines:",
                "second",
                "10:00 <me> Yes.");
        assertThat(irc).extracting(TranscriptFormatTest::startsGroup).containsExactly(true, false, false, true);
    }

    @Test
    void modernHeadsEachGroupOnce() {
        List<RichParagraph> modern = paragraphs(MODERN, CONVERSATION, 0);
        assertThat(plainText(modern)).containsExactly(
                "alice  10:00",
                "Hi!",
                "Two lines:",
                "second",
                "me  10:00",
                "Yes.");
        assertThat(modern).extracting(TranscriptFormatTest::startsGroup).containsExactly(true, false, false, false, true, false);
    }

    @Test
    void appendedMessagesAreGroupedWithTheOneBefore() {
        assertThat(plainText(paragraphs(MODERN, CONVERSATION, 1))).containsExactly(
                "Two lines:",
                "second",
                "me  10:00",
                "Yes.");
    }

    @Test
    void segmentsSeparateTimeSenderAndText() {
        RichParagraph line = IRC.paragraphs(CONVERSATION.getFirst(), false).getFirst().paragraph();
        List<String> segments = IntStream.range(0, line.getSegmentCount())
                .mapToObj(i -> line.getSegment(i).getText())
                .toList();
        assertThat(segments).containsExactly("10:00", " ", "<alice>", " ", "Hi!");
    }

    @Test
    void segmentsCarryTheMessageStatesAsStyleNames() {
        ChatMessage mine = CONVERSATION.getLast();
        assertThat(TranscriptSegments.styleNames("message-text", mine, false)).containsExactly("message-text", "outgoing", "sent");
        assertThat(TranscriptSegments.styleNames("message-text", CONVERSATION.getFirst(), true))
                .containsExactly("message-text", "incoming", "sent", "continued");
    }

    @Test
    void markdownLinksAreFoundByOffsetAfterTheIrcPrefix() {
        RenderContext markdown = new RenderContext(_ -> "10:00", MessageRenderer::markdown, () -> null);
        ChatMessage message = new ChatMessage("bob", "see [the docs](https://example.org) now", T0, INCOMING);
        TranscriptLine line = new IrcTranscript(markdown).paragraphs(message, false).getFirst();
        assertThat(line.paragraph().getPlainText()).isEqualTo("10:00 <bob> see the docs now");
        int docs = line.paragraph().getPlainText().indexOf("the docs");
        assertThat(line.linkAt(docs)).isEqualTo("https://example.org");
        assertThat(line.linkAt(docs + "the docs".length() - 1)).isEqualTo("https://example.org");
        assertThat(line.linkAt(docs - 1)).isNull();
        assertThat(line.linkAt(docs + "the docs".length())).isNull();
    }

    @Test
    void emptyLinesStayParagraphs() {
        ChatMessage gap = new ChatMessage("bob", "a\n\nb", T0, INCOMING);
        assertThat(plainText(paragraphs(MODERN, List.of(gap), 0))).containsExactly("bob  10:00", "a", "", "b");
        assertThat(plainText(paragraphs(IRC, List.of(gap), 0))).containsExactly("10:00 <bob> a", "", "b");
    }

    @Test
    void paragraphCountAgreesWithTheParagraphs() {
        for (TranscriptFormat format : List.<TranscriptFormat>of(IRC, MODERN)) {
            for (boolean continued : List.of(false, true)) {
                for (ChatMessage message : List.of(CONVERSATION.get(0), CONVERSATION.get(1), new ChatMessage("bob", "a\n\nb\r\n", T0, INCOMING))) {
                    assertThat(format.paragraphCount(message, continued))
                            .as("%s, continued %s, %s", format.getClass().getSimpleName(), continued, message.text())
                            .isEqualTo(format.paragraphs(message, continued).size());
                }
            }
        }
    }
}
