package org.jabref.chatpane.skin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatMessage.Direction;
import org.jabref.chatpane.ChatMessage.Status;
import org.jabref.chatpane.MessageAction;

/// One row of the [org.jabref.chatpane.MessageLayout#BUBBLES] layout: a talk bubble,
/// left for incoming, right for outgoing messages. (IRC and MODERN are the transcript, see
/// [ChatPaneSkin].) Builds its graphic from scratch on every update: cells are recycled by the
/// [javafx.scene.control.ListView], so only the visible handful ever exists.
///
/// CSS hooks, following the JavaFX convention of style classes for structure and pseudo-classes
/// for state: style class `message-cell` on the cell (which is also a standard `list-cell`);
/// `message-bubble` on the bubble; `message-sender`, `message-time` on the labels (also standard
/// `label`s); `message-body` on the body, a read-only [BubbleText] (also a standard
/// `rich-text-area`, so selection and *Copy* work), whose text carries the style names of
/// [TranscriptSegments].
/// Pseudo-classes on the cell: `:outgoing` or `:incoming`; `:sent`, `:pending` or `:error`; and
/// `:continued` for a message that continues its sender's group (no sender name).
/// The pane's [MessageAction]s for the message sit as `message-action` buttons in a
/// `message-actions` box on the inner side of the bubble, shown while the pointer is over the row;
/// the time label's tooltip gives date and time in full.
// [impl->dsn~message-cell-bubbles~3]
// [impl->dsn~message-actions~2]
final class MessageCell extends ListCell<ChatMessage> {

    private static final PseudoClass CONTINUED = PseudoClass.getPseudoClass("continued");

    private static final Map<Direction, PseudoClass> DIRECTIONS = new EnumMap<>(Direction.class);
    private static final Map<Status, PseudoClass> STATUSES = new EnumMap<>(Status.class);

    static {
        for (Direction direction : Direction.values()) {
            DIRECTIONS.put(direction, PseudoClass.getPseudoClass(direction.cssName()));
        }
        for (Status status : Status.values()) {
            STATUSES.put(status, PseudoClass.getPseudoClass(status.cssName()));
        }
    }

    /// Share of the list width a bubble may take, so short replies do not stretch edge to edge.
    private static final double BUBBLE_WIDTH_SHARE = 0.7;

    private final RenderContext context;

    MessageCell(RenderContext context) {
        this.context = context;
        getStyleClass().add("message-cell");
        // Without this the cell asks for its content's preferred width, and the list grows a
        // horizontal scroll bar instead of wrapping long messages.
        setPrefWidth(0);
    }

    @Override
    protected void updateItem(@Nullable ChatMessage message, boolean empty) {
        super.updateItem(message, empty);
        setText(null);
        if (empty || message == null) {
            setGraphic(null);
            pseudoClassStateChanged(CONTINUED, false);
            DIRECTIONS.values().forEach(pseudoClass -> pseudoClassStateChanged(pseudoClass, false));
            STATUSES.values().forEach(pseudoClass -> pseudoClassStateChanged(pseudoClass, false));
            return;
        }
        boolean continued = MessageGrouping.continuesGroup(previous(), message);
        pseudoClassStateChanged(CONTINUED, continued);
        DIRECTIONS.forEach((direction, pseudoClass) -> pseudoClassStateChanged(pseudoClass, direction == message.direction()));
        STATUSES.forEach((status, pseudoClass) -> pseudoClassStateChanged(pseudoClass, status == message.status()));
        setGraphic(bubble(message, continued));
    }

    private @Nullable ChatMessage previous() {
        int index = getIndex();
        return index > 0 && getListView() != null ? getListView().getItems().get(index - 1) : null;
    }

    private HBox bubble(ChatMessage message, boolean continued) {
        VBox bubble = new VBox();
        bubble.getStyleClass().add("message-bubble");
        boolean outgoing = message.direction() == Direction.OUTGOING;
        if (!outgoing && !continued) {
            bubble.getChildren().add(label(message.sender(), "message-sender"));
        }
        Label sentAt = label(context.time(message.sentAt()), "message-time");
        sentAt.setTooltip(new Tooltip(context.fullTime(message.sentAt())));
        sentAt.setMaxWidth(Double.MAX_VALUE);
        sentAt.setAlignment(Pos.CENTER_RIGHT);
        BubbleText body = new BubbleText(message, context);
        bubble.getChildren().addAll(body, sentAt, body.measuringNode());
        if (getListView() != null) {
            bubble.maxWidthProperty().bind(getListView().widthProperty().multiply(BUBBLE_WIDTH_SHARE));
        }

        HBox row = new HBox(bubble);
        row.setAlignment(outgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        List<MessageAction> actions = context.actionsFor(message);
        if (!actions.isEmpty()) {
            // On the inner side of the bubble, as in JabRef's AI chat; always laid out, only shown
            // under the pointer, so the bubble does not jump when they appear.
            HBox buttons = actionButtons(actions, message);
            buttons.visibleProperty().bind(hoverProperty());
            row.getChildren().add(outgoing ? 0 : 1, buttons);
        }
        return row;
    }

    private static HBox actionButtons(List<MessageAction> actions, ChatMessage message) {
        HBox buttons = new HBox();
        buttons.getStyleClass().add("message-actions");
        buttons.setAlignment(Pos.CENTER);
        for (MessageAction action : actions) {
            Button button = new Button();
            button.getStyleClass().add("message-action");
            var graphic = action.graphic();
            if (graphic != null) {
                button.setGraphic(graphic.get());
                button.setTooltip(new Tooltip(action.text()));
            } else {
                button.setText(action.text());
            }
            button.setFocusTraversable(false);
            button.setOnAction(_ -> action.onAction().accept(message));
            buttons.getChildren().add(button);
        }
        return buttons;
    }

    private static Label label(String content, String styleClass) {
        Label label = new Label(content);
        label.getStyleClass().add(styleClass);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }
}
