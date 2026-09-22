package org.jabref.chatpane;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.scene.Node;

import org.jspecify.annotations.Nullable;

/// Something the user can do with one message — delete it, answer it again, … — offered in its
/// context menu in every layout and, in [MessageLayout#BUBBLES], as a button next to the bubble
/// while the pointer is over it. Add actions to [ChatPane#getMessageActions()].
///
/// *Copy* and *Select All* need no action: every message text offers them already.
///
/// @param text      the menu item's and the button's text (the button's tooltip if it has a graphic)
/// @param graphic   makes a fresh graphic for each menu item and button (a node can only be shown
///                  once), or `null` for text only
/// @param appliesTo whether the action is offered for a message, e.g. *Retry* only for
///                  [ChatMessage.Status#ERROR]
/// @param onAction  what it does; gets the message instance from [ChatPane#getMessages()], so
///                  `getMessages().indexOf` or identity finds it
public record MessageAction(String text, @Nullable Supplier<Node> graphic, Predicate<ChatMessage> appliesTo,
        Consumer<ChatMessage> onAction) {

    public MessageAction {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(appliesTo, "appliesTo");
        Objects.requireNonNull(onAction, "onAction");
    }

    /// An action with text only, offered for every message.
    public static MessageAction of(String text, Consumer<ChatMessage> onAction) {
        return new MessageAction(text, null, _ -> true, onAction);
    }

    /// This action, offered only where `condition` holds (and it did before).
    public MessageAction onlyFor(Predicate<ChatMessage> condition) {
        return new MessageAction(text, graphic, appliesTo.and(condition), onAction);
    }

    /// This action with a graphic.
    public MessageAction withGraphic(Supplier<Node> newGraphic) {
        return new MessageAction(text, newGraphic, appliesTo, onAction);
    }
}
