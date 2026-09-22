package org.jabref.chatpane.skin;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;

import org.jspecify.annotations.Nullable;

/// Makes the links in a [RichTextArea] over a [TranscriptModel] clickable: a hand cursor over a
/// link while the pane has a link handler, and a plain click (no drag, no selection) hands its
/// target to the handler. Used by the transcript and by every bubble body alike.
// [impl->dsn~message-links~1]
final class LinkInteraction {

    private LinkInteraction() {
    }

    static void install(RichTextArea area, RenderContext context) {
        area.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            boolean onLink = context.linksActive() && linkAt(area, event) != null;
            // On the content node: it carries the text cursor from the theme, which would win
            // over a cursor on the area itself.
            Node content = area.lookup(".content");
            if (content != null) {
                content.setCursor(onLink ? Cursor.HAND : null);
            }
        });
        area.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || !event.isStillSincePress() || event.getClickCount() != 1
                    || hasSelection(area)) {
                return;
            }
            String target = linkAt(area, event);
            if (target != null) {
                context.openLink(target);
                event.consume();
            }
        });
    }

    private static @Nullable String linkAt(RichTextArea area, MouseEvent event) {
        if (!(area.getModel() instanceof TranscriptModel model)) {
            return null;
        }
        @Nullable TextPos pos = area.getTextPosition(event.getScreenX(), event.getScreenY());
        return pos == null ? null : model.linkAt(pos);
    }

    private static boolean hasSelection(RichTextArea area) {
        SelectionSegment selection = area.getSelection();
        return selection != null && !selection.isCollapsed();
    }
}
