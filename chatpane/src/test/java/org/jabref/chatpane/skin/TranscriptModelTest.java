package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ContentChange;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.MessageRenderer;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.assertj.core.api.Assertions.assertThat;

// [utest->dsn~transcript-model~4]
class TranscriptModelTest {

    private static final Instant T0 = Instant.parse("2026-09-22T10:00:00Z");

    /// An IRC format that counts the messages it builds paragraphs for.
    private static final class CountingFormat implements TranscriptFormat {

        private final IrcTranscript irc = new IrcTranscript(TranscriptFormatTest.PLAIN);
        private final List<String> built = new ArrayList<>();

        @Override
        public List<TranscriptLine> paragraphs(ChatMessage message, boolean continued) {
            built.add(message.text());
            return irc.paragraphs(message, continued);
        }

        @Override
        public int paragraphCount(ChatMessage message, boolean continued) {
            return irc.paragraphCount(message, continued);
        }
    }

    /// Messages an hour apart, so none continues a group: `text` per message.
    private static List<ChatMessage> messages(String... texts) {
        return IntStream.range(0, texts.length)
                .mapToObj(i -> new ChatMessage("bob", texts[i], T0.plusSeconds(3600L * i), INCOMING))
                .toList();
    }

    private static List<String> lines(TranscriptModel model) {
        return IntStream.range(0, model.size()).mapToObj(model::getPlainText).toList();
    }

    /// Index and offset only: a position's charIndex and leading flag are the model's business.
    private static List<Integer> position(TextPos pos) {
        return List.of(pos.index(), pos.offset());
    }

    private static List<ContentChange> recordChanges(TranscriptModel model) {
        List<ContentChange> changes = new ArrayList<>();
        model.addListener(changes::add);
        return changes;
    }

    @Test
    void emptyTranscriptIsOneEmptyParagraph() {
        TranscriptModel model = new TranscriptModel(new CountingFormat(), List.of());
        assertThat(lines(model)).containsExactly("");
        assertThat(model.isWritable()).isFalse();
    }

    @Test
    void paragraphsFollowTheMessagesAndTheirLines() {
        TranscriptModel model = new TranscriptModel(new CountingFormat(), messages("one", "two\nlines", "three"));
        assertThat(lines(model)).containsExactly("10:00 <bob> one", "10:00 <bob> two", "lines", "10:00 <bob> three");
    }

    @Test
    void buildsOnlyTheMessagesAskedFor() {
        CountingFormat format = new CountingFormat();
        TranscriptModel model = new TranscriptModel(format, messages("a", "b\nb2", "c", "d"));
        assertThat(model.size()).isEqualTo(5);
        assertThat(format.built).as("counting builds nothing").isEmpty();
        model.getPlainText(3);
        model.getParagraph(2);
        model.getPlainText(3);
        assertThat(format.built).as("built once, then cached").containsExactly("c", "b\nb2");
    }

    @Test
    void firstAppendReplacesThePlaceholder() {
        TranscriptModel model = new TranscriptModel(new CountingFormat(), List.of());
        List<ContentChange> changes = recordChanges(model);
        List<ChatMessage> all = messages("one", "two");
        model.append(all, 0);
        assertThat(lines(model)).containsExactly("10:00 <bob> one", "10:00 <bob> two");
        assertThat(changes).singleElement().satisfies(change -> {
            assertThat(position(change.getStart())).containsExactly(0, 0);
            assertThat(change.getCharsAddedTop()).isEqualTo("10:00 <bob> one".length());
            assertThat(change.getLinesAdded()).isEqualTo(1);
            assertThat(change.getCharsAddedBottom()).isEqualTo("10:00 <bob> two".length());
        });
    }

    @Test
    void appendAddsLinesAfterTheEnd() {
        List<ChatMessage> all = messages("one", "two", "three!");
        TranscriptModel model = new TranscriptModel(new CountingFormat(), all.subList(0, 1));
        List<ContentChange> changes = recordChanges(model);
        model.append(all, 1);
        assertThat(lines(model)).containsExactly("10:00 <bob> one", "10:00 <bob> two", "10:00 <bob> three!");
        assertThat(changes).singleElement().satisfies(change -> {
            assertThat(position(change.getStart())).containsExactly(0, "10:00 <bob> one".length());
            assertThat(change.getCharsAddedTop()).isZero();
            assertThat(change.getLinesAdded()).isEqualTo(2);
            assertThat(change.getCharsAddedBottom()).isEqualTo("10:00 <bob> three!".length());
        });
        assertThat(position(model.getDocumentEnd())).containsExactly(2, "10:00 <bob> three!".length());
    }

    @Test
    void appendGroupsWithTheMessageBefore() {
        List<ChatMessage> all = List.of(
                new ChatMessage("bob", "first", T0, INCOMING),
                new ChatMessage("bob", "second", T0.plusSeconds(10), INCOMING));
        TranscriptModel model = new TranscriptModel(new ModernTranscript(TranscriptFormatTest.PLAIN), all.subList(0, 1));
        model.append(all, 1);
        assertThat(lines(model)).containsExactly("bob  10:00", "first", "second");
    }

    @Test
    void linksAreFoundThroughTheModel() {
        RenderContext markdown = new RenderContext(_ -> "10:00", MessageRenderer::markdown, () -> null);
        List<ChatMessage> all = messages("plain", "a [link](https://example.org)");
        TranscriptModel model = new TranscriptModel(new BodyFormat(markdown), all);
        assertThat(model.linkAt(TextPos.ofLeading(1, 3))).isEqualTo("https://example.org");
        assertThat(model.linkAt(TextPos.ofLeading(1, 0))).isNull();
        assertThat(model.linkAt(TextPos.ofLeading(0, 1))).isNull();
        assertThat(model.linkAt(TextPos.ofLeading(7, 0))).as("past the end").isNull();
    }

    @Test
    void updateReplacesOneMessagesParagraphsInPlace() {
        List<ChatMessage> all = new ArrayList<>(messages("one", "two", "three"));
        TranscriptModel model = new TranscriptModel(new CountingFormat(), all);
        List<ContentChange> changes = recordChanges(model);
        all.set(1, all.get(1).withText("two\nand more"));
        assertThat(model.update(all, 1)).isTrue();
        assertThat(lines(model)).containsExactly("10:00 <bob> one", "10:00 <bob> two", "and more", "10:00 <bob> three");
        assertThat(changes).singleElement().satisfies(change -> {
            assertThat(position(change.getStart())).containsExactly(1, 0);
            assertThat(position(change.getEnd())).containsExactly(1, "10:00 <bob> two".length());
            assertThat(change.getCharsAddedTop()).isEqualTo("10:00 <bob> two".length());
            assertThat(change.getLinesAdded()).isEqualTo(1);
            assertThat(change.getCharsAddedBottom()).isEqualTo("and more".length());
        });
        assertThat(model.messageAt(TextPos.ofLeading(3, 0))).isEqualTo(all.get(2));
    }

    @Test
    void updateThatChangesTheNextMessagesGroupAsksForARebuild() {
        List<ChatMessage> all = new ArrayList<>(List.of(
                new ChatMessage("bob", "first", T0, INCOMING),
                new ChatMessage("bob", "second", T0.plusSeconds(10), INCOMING)));
        TranscriptModel model = new TranscriptModel(new ModernTranscript(TranscriptFormatTest.PLAIN), all);
        all.set(0, new ChatMessage("alice", "first", T0, INCOMING));
        assertThat(model.update(all, 0)).as("bob's second message now starts a group of its own").isFalse();
        assertThat(lines(model)).as("unchanged").containsExactly("bob  10:00", "first", "second");
    }

    @Test
    void appendingNothingFiresNothing() {
        List<ChatMessage> all = messages("one");
        TranscriptModel model = new TranscriptModel(new CountingFormat(), all);
        List<ContentChange> changes = recordChanges(model);
        model.append(all, 1);
        assertThat(changes).isEmpty();
    }
}
