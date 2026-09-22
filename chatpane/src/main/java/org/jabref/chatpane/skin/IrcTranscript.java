package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.List;

import jfx.incubator.scene.control.richtext.model.RichParagraph;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.TextLine;

import static org.jabref.chatpane.skin.TranscriptSegments.addLine;
import static org.jabref.chatpane.skin.TranscriptSegments.names;
import static org.jabref.chatpane.skin.TranscriptSegments.paragraphFor;

/// [org.jabref.chatpane.MessageLayout#IRC]: the first line of a message as
/// `time <sender> text`, every further rendered line a paragraph of its own.
// [impl->dsn~transcript-paragraphs~4]
final class IrcTranscript implements TranscriptFormat {

    private final RenderContext context;

    IrcTranscript(RenderContext context) {
        this.context = context;
    }

    @Override
    public List<TranscriptLine> paragraphs(ChatMessage message, boolean continued) {
        List<TextLine> lines = context.lines(message);
        String time = context.time(message.sentAt());
        String sender = "<" + message.sender() + ">";
        RichParagraph.Builder first = paragraphFor(lines.getFirst(), !continued)
                .addWithStyleNames(time, names("message-time", message, continued))
                .addSegment(" ")
                .addWithStyleNames(sender, names("message-sender", message, continued))
                .addSegment(" ");
        int prefix = time.length() + 1 + sender.length() + 1;
        List<TranscriptLine> paragraphs = new ArrayList<>();
        List<TranscriptLine.Link> links = addLine(first, prefix, lines.getFirst(), message, continued);
        paragraphs.add(new TranscriptLine(first.build(), links));
        for (TextLine line : lines.subList(1, lines.size())) {
            RichParagraph.Builder paragraph = paragraphFor(line, false);
            List<TranscriptLine.Link> lineLinks = addLine(paragraph, 0, line, message, continued);
            paragraphs.add(new TranscriptLine(paragraph.build(), lineLinks));
        }
        return paragraphs;
    }

    @Override
    public int paragraphCount(ChatMessage message, boolean continued) {
        return context.lineCount(message);
    }
}
