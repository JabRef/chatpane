package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.List;

import jfx.incubator.scene.control.richtext.model.RichParagraph;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.TextLine;

import static org.jabref.chatpane.skin.TranscriptSegments.addLine;
import static org.jabref.chatpane.skin.TranscriptSegments.paragraphFor;

/// The rendered lines of a message and nothing else: the body of a bubble ([BubbleText]) and of a
/// modern entry ([ModernTranscript]) — one format for both, so a message reads the same in either.
// [impl->dsn~transcript-paragraphs~4]
final class BodyFormat implements TranscriptFormat {

    private final RenderContext context;

    BodyFormat(RenderContext context) {
        this.context = context;
    }

    @Override
    public List<TranscriptLine> paragraphs(ChatMessage message, boolean continued) {
        List<TranscriptLine> paragraphs = new ArrayList<>();
        for (TextLine line : context.lines(message)) {
            RichParagraph.Builder paragraph = paragraphFor(line, false);
            List<TranscriptLine.Link> links = addLine(paragraph, 0, line, message, continued);
            paragraphs.add(new TranscriptLine(paragraph.build(), links));
        }
        return paragraphs;
    }

    @Override
    public int paragraphCount(ChatMessage message, boolean continued) {
        return context.lineCount(message);
    }
}
