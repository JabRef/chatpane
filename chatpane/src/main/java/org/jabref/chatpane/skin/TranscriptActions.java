package org.jabref.chatpane.skin;

import java.util.List;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SideDecorator;
import jfx.incubator.scene.control.richtext.TextPos;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.MessageAction;

/// The pane's [MessageAction]s as buttons at the right of the transcript, beside the first line of
/// the message under the pointer — what the bubbles show beside the bubble ([MessageCell]).
///
/// The area's right [SideDecorator] column holds them: always as wide as the buttons of all the
/// pane's actions, so the text does not reflow when they appear, and only there while the pane has
/// actions ([#update()]).
// [impl->dsn~message-actions~4]
final class TranscriptActions implements SideDecorator {

    /// The message under the pointer and its first paragraph.
    private record Hover(int paragraph, ChatMessage message) {
    }

    private final RichTextArea area;
    private final RenderContext context;
    private final ObjectProperty<@Nullable Hover> hover = new SimpleObjectProperty<>();

    /// Where the pointer was last seen over the area, in screen coordinates; NaN outside.
    private double screenX = Double.NaN;
    private double screenY = Double.NaN;

    TranscriptActions(RichTextArea area, RenderContext context) {
        this.area = area;
        this.context = context;
        area.addEventFilter(MouseEvent.MOUSE_MOVED, event -> track(event.getScreenX(), event.getScreenY()));
        area.addEventHandler(MouseEvent.MOUSE_EXITED, _ -> track(Double.NaN, Double.NaN));
        // The text moves under a resting pointer: look again once the scroll is laid out.
        area.addEventFilter(ScrollEvent.SCROLL, _ -> Platform.runLater(this::update));
    }

    /// Shows the column while the pane has actions, and the buttons for the message now under the
    /// pointer — after the messages or the actions changed.
    void update() {
        area.setRightDecorator(context.actions().isEmpty() ? null : this);
        track(screenX, screenY);
    }

    private void track(double x, double y) {
        screenX = x;
        screenY = y;
        Hover next = null;
        if (!Double.isNaN(x) && area.getModel() instanceof TranscriptModel model) {
            TextPos pos = area.getTextPosition(x, y);
            ChatMessage message = pos == null ? null : model.messageAt(pos);
            if (message != null) {
                next = new Hover(model.messageStartAt(pos), message);
            }
        }
        // Not on every move: a new hover rebuilds the buttons.
        if (!Objects.equals(hover.get(), next)) {
            hover.set(next);
        }
    }

    /// No fixed width: the area measures [#getMeasurementNode(int)].
    @Override
    public double getPrefWidth(double viewWidth) {
        return 0;
    }

    /// The buttons of all the pane's actions, the widest the column has to hold.
    @Override
    public Node getMeasurementNode(int index) {
        return ActionButtons.of(context.actions(), _ -> {
        });
    }

    /// A slot beside paragraph `index` that holds the buttons while the message starting there is
    /// under the pointer.
    ///
    /// The area keeps the slots by paragraph index for as long as the decorator is set, across
    /// model changes, so a slot stands for a position and looks up the message only when it fills.
    @Override
    public Node getNode(int index) {
        StackPane slot = new StackPane();
        slot.setAlignment(Pos.TOP_RIGHT);
        InvalidationListener fill = _ -> fill(slot, index);
        // The slot keeps its listener alive; the hover holds it weakly, so a dropped slot goes.
        slot.getProperties().put(TranscriptActions.class, fill);
        hover.addListener(new WeakInvalidationListener(fill));
        fill(slot, index);
        return slot;
    }

    private void fill(StackPane slot, int index) {
        Hover current = hover.get();
        if (current == null || current.paragraph() != index) {
            slot.getChildren().clear();
            return;
        }
        ChatMessage message = current.message();
        List<MessageAction> actions = context.actionsFor(message);
        slot.getChildren().setAll(ActionButtons.of(actions, action -> action.getOnAction().accept(message)));
    }
}
