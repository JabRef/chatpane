package org.jabref.chatpane;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jspecify.annotations.Nullable;

/// Something the user can do with one message — delete it, answer it again, … — offered in its
/// context menu in every layout and, in [MessageLayout#BUBBLES], as a button next to the bubble
/// while the pointer is over it. Add actions to [ChatPane#getMessageActions()].
///
/// Like a [javafx.scene.control.MenuItem], an action describes the controls the pane makes from
/// it, and they follow its properties while shown: change [#textProperty()], bind
/// [#disableProperty()] to the application's state, and every menu item and button of the action
/// updates.
///
/// Which messages an action is offered for is fixed: [#getAppliesTo()] is asked each time a
/// message is shown, and a message changes only by being replaced in [ChatPane#getMessages()].
/// So the predicate looks at the message alone; state outside the message belongs in
/// [#disableProperty()] or [#visibleProperty()].
///
/// *Copy* and *Select All* need no action: every message text offers them already.
public final class MessageAction {

    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final ObjectProperty<@Nullable Supplier<Node>> graphic = new SimpleObjectProperty<>(this, "graphic");
    private final BooleanProperty disable = new SimpleBooleanProperty(this, "disable");
    private final BooleanProperty visible = new SimpleBooleanProperty(this, "visible", true);
    private final ObservableList<String> styleClass = FXCollections.observableArrayList();
    private final Predicate<ChatMessage> appliesTo;
    private final Consumer<ChatMessage> onAction;

    /// An action offered for every message.
    ///
    /// @param text     the menu item's and the button's text, see [#textProperty()]
    /// @param onAction what it does, see [#getOnAction()]
    public MessageAction(String text, Consumer<ChatMessage> onAction) {
        this(text, _ -> true, onAction);
    }

    /// An action offered only for the messages `appliesTo` accepts, e.g. *Retry* only for
    /// [ChatMessage.Status#ERROR].
    ///
    /// @param text      the menu item's and the button's text, see [#textProperty()]
    /// @param appliesTo whether the action is offered for a message, see [#getAppliesTo()]
    /// @param onAction  what it does, see [#getOnAction()]
    public MessageAction(String text, Predicate<ChatMessage> appliesTo, Consumer<ChatMessage> onAction) {
        setText(text);
        this.appliesTo = Objects.requireNonNull(appliesTo, "appliesTo");
        this.onAction = Objects.requireNonNull(onAction, "onAction");
    }

    /// The menu item's and the button's text; with a graphic, the button shows the graphic only
    /// and the text as its tooltip (screen readers still read the text). Shown as given: the
    /// application translates it. `null` reads as empty.
    public StringProperty textProperty() {
        return text;
    }

    public String getText() {
        return Objects.requireNonNullElse(text.get(), "");
    }

    public void setText(String newText) {
        text.set(Objects.requireNonNull(newText, "text"));
    }

    /// Makes the graphic of the menu item and of the button: called once for each, since a node can
    /// only be shown in one place. `null` (default): text only.
    public ObjectProperty<@Nullable Supplier<Node>> graphicProperty() {
        return graphic;
    }

    public @Nullable Supplier<Node> getGraphic() {
        return graphic.get();
    }

    public void setGraphic(@Nullable Supplier<Node> newGraphic) {
        graphic.set(newGraphic);
    }

    /// Whether the menu item and the button are shown greyed out and do nothing when clicked —
    /// for an action the application cannot do right now. Default `false`.
    public BooleanProperty disableProperty() {
        return disable;
    }

    public boolean isDisable() {
        return disable.get();
    }

    public void setDisable(boolean newDisable) {
        disable.set(newDisable);
    }

    /// Whether the action is shown at all; a hidden one leaves no gap and keeps its place in
    /// [ChatPane#getMessageActions()]. Default `true`.
    public BooleanProperty visibleProperty() {
        return visible;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public void setVisible(boolean newVisible) {
        visible.set(newVisible);
    }

    /// Style classes the menu item and the button carry besides `message-action`, so a stylesheet
    /// can tell one action from another. Empty by default.
    public ObservableList<String> getStyleClass() {
        return styleClass;
    }

    /// Whether the action is offered for a message; looks at the message alone (see the class
    /// documentation).
    public Predicate<ChatMessage> getAppliesTo() {
        return appliesTo;
    }

    /// What the action does; gets the message instance from [ChatPane#getMessages()], so
    /// `getMessages().indexOf` or identity finds it.
    public Consumer<ChatMessage> getOnAction() {
        return onAction;
    }

    @Override
    public String toString() {
        return "MessageAction[" + getText() + "]";
    }
}
