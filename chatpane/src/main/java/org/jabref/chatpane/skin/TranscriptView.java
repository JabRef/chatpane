package org.jabref.chatpane.skin;

import java.util.List;

import javafx.scene.Node;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.FindMatch;

/// The [org.jabref.chatpane.MessageLayout#IRC] and
/// [org.jabref.chatpane.MessageLayout#MODERN] view: one read-only [RichTextArea] holding
/// the whole conversation as a [TranscriptModel] (MADR 0010), so a selection can run across
/// messages and copies as a plain-text log. The [TranscriptFormat] decides how a message reads;
/// [TranscriptActions] puts the message actions at the right of the line under the pointer.
// [impl->dsn~transcript-view~5]
final class TranscriptView implements ConversationView {

    private final RichTextArea area = new RichTextArea();
    private final TranscriptFormat format;
    private final TranscriptActions actions;

    TranscriptView(TranscriptFormat format, RenderContext context) {
        this.format = format;
        LinkInteraction.install(area, context);
        MessageMenu.install(area, context, event -> {
            TextPos pos = area.getTextPosition(event.getScreenX(), event.getScreenY());
            return pos != null && area.getModel() instanceof TranscriptModel model ? model.messageAt(pos) : null;
        });
        actions = new TranscriptActions(area, context);
        area.getStyleClass().add("chat-pane-transcript");
        area.setEditable(false);
        area.setFocusTraversable(false);
        // Workaround W6 (docs/workarounds.md): in code, not in chatpane.css — a CSS pass that sets
        // -fx-wrap-text again (any stylesheet change) makes the area lay out in the middle of the
        // pass and fail with "duplicate children added".
        area.setWrapText(true);
        area.setDisplayCaret(false);
        area.setHighlightCurrentParagraph(false);
    }

    @Override
    public Node node() {
        return area;
    }

    @Override
    public void show(List<ChatMessage> messages) {
        replaced(messages);
    }

    @Override
    public void hide() {
        area.setModel(null);
        actions.update();
    }

    @Override
    public void appended(List<ChatMessage> messages, int from) {
        if (area.getModel() instanceof TranscriptModel model) {
            model.append(messages, from);
            followEnd();
            actions.update();
        }
    }

    @Override
    public void updated(List<ChatMessage> messages, int index) {
        if (area.getModel() instanceof TranscriptModel model && model.update(messages, index)) {
            if (index == messages.size() - 1) {
                followEnd();
            }
            actions.update();
        } else {
            replaced(messages);
        }
    }

    @Override
    public void replaced(List<ChatMessage> messages) {
        area.setModel(new TranscriptModel(format, messages));
        followEnd();
        actions.update();
    }

    /// Drops the area's built text cells, whose styles were resolved from the CSS of their time
    /// (Workaround W7); the next layout builds them again.
    @Override
    public void restyle() {
        if (area.getSkin() instanceof RichTextAreaSkin skin) {
            skin.refreshLayout();
        }
    }

    /// The built paragraphs carry the highlights: build them again, then draw them again.
    // [impl->dsn~find-highlights~1]
    @Override
    public void findChanged() {
        if (area.getModel() instanceof TranscriptModel model) {
            model.forgetBuilt();
            restyle();
        }
    }

    /// Selects the match, which scrolls to it — the area has no other public way to scroll — and
    /// keeps the view from following the newest message away from it, as any selection does.
    // [impl->dsn~find-reveal~1]
    @Override
    public void reveal(FindMatch match) {
        if (area.getModel() instanceof TranscriptModel model) {
            TextPos start = model.locate(match.message(), match.line(), match.start());
            TextPos end = model.locate(match.message(), match.line(), match.end());
            if (start != null && end != null) {
                area.select(start, end);
            }
        }
    }

    /// Moves the (hidden) caret to the end, which scrolls there, unless text is selected.
    private void followEnd() {
        StyledTextModel model = area.getModel();
        if (!hasSelection() && model != null) {
            area.select(model.getDocumentEnd());
        }
    }

    private boolean hasSelection() {
        SelectionSegment selection = area.getSelection();
        return selection != null && !selection.isCollapsed();
    }
}
