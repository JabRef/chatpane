package org.jabref.chatpane.skin;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import org.jabref.chatpane.MessageAction;

/// The `message-actions` box of `message-action` buttons, beside a bubble ([MessageCell]) or at
/// the right of a transcript line ([TranscriptActions]); each button made and bound by
/// [ActionControls], so it follows its action.
// [impl->dsn~message-actions~4]
final class ActionButtons {

    private ActionButtons() {
    }

    /// A box with one button per action; a button hands its action to `onAction`.
    static HBox of(List<MessageAction> actions, Consumer<MessageAction> onAction) {
        HBox buttons = new HBox();
        buttons.getStyleClass().add("message-actions");
        buttons.setAlignment(Pos.CENTER);
        for (MessageAction action : actions) {
            buttons.getChildren().add(ActionControls.button(action, () -> onAction.accept(action)));
        }
        return buttons;
    }
}
