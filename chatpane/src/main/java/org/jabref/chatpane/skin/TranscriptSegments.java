package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.List;

import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.TextLine;
import org.jabref.chatpane.TextSpan;

/// Building blocks of the transcript and bubble-body formats.
///
/// Segments carry CSS style names only, never a color (MADR 0007): the part's own name
/// (`message-time`, `message-sender`, `message-text`), `incoming` or `outgoing`, `sent`, `pending` or
/// `error`, `continued` for a message that continues its group — the states a bubble cell shows as
/// pseudo-classes — and, for
/// rendered text, the line's kind (`line-paragraph`, `line-heading` plus `line-heading-2`,
/// `line-quote`, `line-list-item`, `line-code-block`) and the span's styles (`span-bold`,
/// `span-italic`, `span-code`, `span-strikethrough`, `span-link`).
final class TranscriptSegments {

    /// Space above a paragraph that starts a group, in pixels (the model's unit).
    static final double GROUP_GAP = 6;

    /// Space above a line that starts a new block inside a message (a paragraph after a list, …).
    static final double BLOCK_GAP = 3;

    /// Indentation per nesting level of a list item or quote.
    static final double INDENT = 16;

    private TranscriptSegments() {
    }

    /// The style names of one part of `message`.
    static List<String> styleNames(String part, ChatMessage message, boolean continued) {
        List<String> names = new ArrayList<>(List.of(part, message.direction().cssName(), message.status().cssName()));
        if (continued) {
            names.add("continued");
        }
        return names;
    }

    static String[] names(String part, ChatMessage message, boolean continued) {
        return styleNames(part, message, continued).toArray(String[]::new);
    }

    /// Starts a paragraph for a rendered `line`: its indentation and the space above it —
    /// [#GROUP_GAP] if it starts a group, else [#BLOCK_GAP] if it starts a block.
    static RichParagraph.Builder paragraphFor(TextLine line, boolean startsGroup) {
        StyleAttributeMap.Builder attributes = StyleAttributeMap.builder();
        if (startsGroup) {
            attributes.setSpaceAbove(GROUP_GAP);
        } else if (line.startsBlock()) {
            attributes.setSpaceAbove(BLOCK_GAP);
        }
        boolean nests = line.kind() == TextLine.Kind.LIST_ITEM || line.kind() == TextLine.Kind.QUOTE;
        if (nests && line.level() > 0) {
            attributes.setSpaceLeft(INDENT * line.level());
        }
        return RichParagraph.builder().setParagraphAttributes(attributes.build());
    }

    /// Adds the spans of `line` to `paragraph`, which already holds `offset` characters (an IRC
    /// prefix), and returns the links among them.
    static List<TranscriptLine.Link> addLine(RichParagraph.Builder paragraph, int offset, TextLine line,
            ChatMessage message, boolean continued) {
        List<TranscriptLine.Link> links = new ArrayList<>();
        int position = offset;
        for (TextSpan span : line.spans()) {
            List<String> names = styleNames("message-text", message, continued);
            names.add("line-" + line.kind().cssName());
            if (line.kind() == TextLine.Kind.HEADING) {
                names.add("line-heading-" + line.level());
            }
            span.styles().stream().sorted().forEach(style -> names.add("span-" + style.cssName()));
            String target = span.link();
            if (target != null) {
                names.add("span-link");
                links.add(new TranscriptLine.Link(position, position + span.text().length(), target));
            }
            paragraph.addWithStyleNames(span.text(), names.toArray(String[]::new));
            position += span.text().length();
        }
        return links;
    }
}
