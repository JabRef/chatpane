package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.List;

import jfx.incubator.scene.control.richtext.model.RichParagraph;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.TextLine;

import static org.jabref.chatpane.skin.TranscriptSegments.names;
import static org.jabref.chatpane.skin.TranscriptSegments.paragraphFor;

/// [org.jabref.chatpane.MessageLayout#MODERN]: a header `sender  time` above each group,
/// then the rendered lines of the message, one paragraph each ([BodyFormat]).
// [impl->dsn~transcript-paragraphs~4]
final class ModernTranscript implements TranscriptFormat {

    private final RenderContext context;
    private final BodyFormat body;

    ModernTranscript(RenderContext context) {
        this.context = context;
        this.body = new BodyFormat(context);
    }

    @Override
    public List<TranscriptLine> paragraphs(ChatMessage message, boolean continued) {
        List<TranscriptLine> paragraphs = new ArrayList<>();
        if (!continued) {
            RichParagraph header = paragraphFor(TextLine.plain(""), true)
                    .addWithStyleNames(message.sender(), names("message-sender", message, false))
                    .addSegment("  ")
                    .addWithStyleNames(context.time(message.sentAt()), names("message-time", message, false))
                    .build();
            paragraphs.add(new TranscriptLine(header, List.of()));
        }
        paragraphs.addAll(body.paragraphs(message, continued));
        return paragraphs;
    }

    @Override
    public int paragraphCount(ChatMessage message, boolean continued) {
        return body.paragraphCount(message, continued) + (continued ? 0 : 1);
    }
}
