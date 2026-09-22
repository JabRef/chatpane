package org.jabref.chatpane.skin;

import java.util.List;

import jfx.incubator.scene.control.richtext.model.RichParagraph;

import org.jspecify.annotations.Nullable;

/// One built paragraph of a transcript or a bubble body, with the link targets of its text —
/// a `RichParagraph` itself carries only style names, so the targets travel alongside.
///
/// @param paragraph what the area shows
/// @param links     the links in it, by character range
record TranscriptLine(RichParagraph paragraph, List<Link> links) {

    /// A link over characters `[start, end)` of the paragraph.
    record Link(int start, int end, String target) {
    }

    TranscriptLine {
        links = List.copyOf(links);
    }

    /// The target of the link at character `offset`, if any.
    @Nullable String linkAt(int offset) {
        for (Link link : links) {
            if (offset >= link.start() && offset < link.end()) {
                return link.target();
            }
        }
        return null;
    }
}
