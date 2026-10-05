package org.jabref.chatpane.skin;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.jabref.chatpane.MessageAction;

/// The `message-actions` box of `message-action` buttons, beside a bubble ([MessageCell]) or at
/// the right of a transcript line ([TranscriptActions]); a graphic makes the text a tooltip.
// [impl->dsn~message-actions~3]
final class ActionButtons {

    private ActionButtons() {
    }

    /// A box with one button per action; a button hands its action to `onAction`.
    static HBox of(List<MessageAction> actions, Consumer<MessageAction> onAction) {
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
            button.setOnAction(_ -> onAction.accept(action));
            buttons.getChildren().add(button);
        }
        return buttons;
    }
}
