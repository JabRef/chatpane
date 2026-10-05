package org.jabref.chatpane.skin;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import jfx.incubator.scene.control.richtext.RichTextArea;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.TextLine;
import org.jabref.chatpane.TextSpan;

/// A message body in a bubble: a read-only [RichTextArea] over a one-message [TranscriptModel] in
/// the [BodyFormat] — the same rendering the modern transcript uses, so selection, *Copy*, the
/// context menu, Markdown styles and links behave alike in every layout.
///
/// Its height comes from the area itself (`-fx-use-content-height`, set in `chatpane.css`). Its
/// width is measured here, so a short message gets a short bubble: the area's own
/// `useContentWidth` stops wrapping, which a long message needs (Workaround W5). A fresh body
/// lays itself out before it reports its height (Workaround W8).
// [impl->dsn~bubble-text~2]
final class BubbleText extends RichTextArea {

    /// Headroom so rounding in the area's own layout never wraps a line the measurement kept.
    private static final double WRAP_SLACK = 3;

    private final List<TextLine> lines;
    private final Group measure = new Group();

    BubbleText(ChatMessage message, RenderContext context) {
        super(new TranscriptModel(new BodyFormat(context), List.of(message)));
        this.lines = context.lines(message);
        getStyleClass().add("message-body");
        setEditable(false);
        // Workaround W6 (docs/workarounds.md): these in code, not in chatpane.css — a CSS pass
        // that sets them again makes the area lay out in the middle of the pass and fail.
        setWrapText(true);
        setUseContentHeight(true);
        setDisplayCaret(false);
        setHighlightCurrentParagraph(false);
        // A conversation of a hundred messages must not become a hundred Tab stops;
        // a click still focuses the text for selecting and copying.
        setFocusTraversable(false);
        LinkInteraction.install(this, context);
        MessageMenu.install(this, context, _ -> message);

        // Workaround W5 (docs/workarounds.md): one Text per span with the span's style names, so
        // the stylesheet gives it the font the area's own text gets; never shown, never laid out.
        for (TextLine line : lines) {
            for (TextSpan span : line.spans()) {
                Text text = new Text(span.text());
                text.getStyleClass().add("message-text");
                text.getStyleClass().add("line-" + line.kind().cssName());
                if (line.kind() == TextLine.Kind.HEADING) {
                    text.getStyleClass().add("line-heading-" + line.level());
                }
                span.styles().forEach(style -> text.getStyleClass().add("span-" + style.cssName()));
                if (span.token() != null) {
                    // A token's style may change its width (a bold keyword).
                    text.getStyleClass().add("token-" + span.token());
                }
                measure.getChildren().add(text);
            }
        }
        measure.setManaged(false);
        measure.setVisible(false);
    }

    /// The measuring texts; the cell puts them next to the body, inside the bubble, where the
    /// same CSS rules reach them.
    Node measuringNode() {
        return measure;
    }

    /// The widest line, unwrapped — at most the bubble's share of the list, which the bubble's
    /// max width enforces.
    // Workaround W5 (docs/workarounds.md).
    @Override
    protected double computePrefWidth(double height) {
        if (measure.getScene() == null) {
            return super.computePrefWidth(height);
        }
        measure.applyCss();
        double widest = 0;
        int text = 0;
        for (TextLine line : lines) {
            double width = indent(line);
            for (int i = 0; i < line.spans().size(); i++, text++) {
                width += measure.getChildren().get(text).getLayoutBounds().getWidth();
            }
            widest = Math.max(widest, width);
        }
        Insets insets = getInsets();
        Insets padding = getContentPadding() == null ? Insets.EMPTY : getContentPadding();
        return Math.ceil(widest) + WRAP_SLACK + insets.getLeft() + insets.getRight() + padding.getLeft() + padding.getRight();
    }

    /// The height of the text at the width the body will get. The area learns its content height
    /// only in its own layout, which a fresh body has not had when the list measures its cell: it
    /// reported its minimum, and the list jumped by the difference for a frame — with every word of
    /// a growing answer, whose cell is built anew each time. So a body never laid out lays itself
    /// out first, at its preferred width within the bubble.
    // Workaround W8 (docs/workarounds.md).
    @Override
    protected double computePrefHeight(double width) {
        if (getWidth() == 0 && getScene() != null && getParent() instanceof Region bubble && bubble.getMaxWidth() > 0) {
            Insets insets = bubble.getInsets();
            resize(Math.min(prefWidth(-1), bubble.getMaxWidth() - insets.getLeft() - insets.getRight()), 0);
            layout();
        }
        return super.computePrefHeight(width);
    }

    private static double indent(TextLine line) {
        boolean nests = line.kind() == TextLine.Kind.LIST_ITEM || line.kind() == TextLine.Kind.QUOTE;
        return nests ? TranscriptSegments.INDENT * line.level() : 0;
    }
}
