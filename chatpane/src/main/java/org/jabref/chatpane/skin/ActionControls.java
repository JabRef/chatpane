package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.MessageAction;

/// Makes the menu items and buttons of a [MessageAction], bound to its properties so they follow
/// it while shown.
///
/// An action usually lives as long as the pane, the controls only as long as a menu or a cell.
/// Property bindings and content listeners therefore must not keep the controls alive: `bind`
/// and the `Bindings` factories observe weakly, and the style-class listener is a
/// [WeakListChangeListener] whose delegate the control itself holds.
// [impl->dsn~message-actions~4]
final class ActionControls {

    private static final String STYLE_CLASS = "message-action";
    private static final String LISTENER_KEY = ActionControls.class.getName() + ".styleClass";

    private ActionControls() {
    }

    /// The context-menu item of `action` for `message`.
    static MenuItem menuItem(MessageAction action, ChatMessage message) {
        MenuItem item = new MenuItem();
        item.textProperty().bind(action.textProperty());
        item.graphicProperty().bind(freshGraphic(action));
        item.disableProperty().bind(action.disableProperty());
        item.visibleProperty().bind(action.visibleProperty());
        item.setOnAction(_ -> action.getOnAction().accept(message));
        followStyleClass(item.getStyleClass(), action, item.getProperties());
        return item;
    }

    /// The button of `action`, calling `onFire` when clicked: the text, or with a graphic the graphic
    /// alone and the text as tooltip — the text stays set either way, so screen readers name the
    /// button.
    static Button button(MessageAction action, Runnable onFire) {
        Button button = new Button();
        button.textProperty().bind(action.textProperty());
        ObjectBinding<@Nullable Node> graphic = freshGraphic(action);
        button.graphicProperty().bind(graphic);
        button.contentDisplayProperty().bind(Bindings.when(graphic.isNull())
                .then(ContentDisplay.TEXT_ONLY).otherwise(ContentDisplay.GRAPHIC_ONLY));
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(action.textProperty());
        button.tooltipProperty().bind(Bindings.when(graphic.isNull())
                .then((Tooltip) null).otherwise(tooltip));
        button.disableProperty().bind(action.disableProperty());
        button.visibleProperty().bind(action.visibleProperty());
        button.managedProperty().bind(action.visibleProperty());
        button.setFocusTraversable(false);
        button.setOnAction(_ -> onFire.run());
        followStyleClass(button.getStyleClass(), action, button.getProperties());
        return button;
    }

    /// Whether any of `actions` is visible, following them — for the separator before them.
    static BooleanBinding anyVisible(List<MessageAction> actions) {
        Observable[] dependencies = actions.stream().map(MessageAction::visibleProperty).toArray(Observable[]::new);
        return Bindings.createBooleanBinding(() -> actions.stream().anyMatch(MessageAction::isVisible), dependencies);
    }

    /// A new node from the action's graphic supplier, made again only when the supplier changes:
    /// the binding caches its value while valid.
    private static ObjectBinding<@Nullable Node> freshGraphic(MessageAction action) {
        return Bindings.createObjectBinding(() -> {
            @Nullable Supplier<Node> supplier = action.getGraphic();
            return supplier == null ? null : supplier.get();
        }, action.graphicProperty());
    }

    /// Keeps `target` at its own classes plus `message-action` plus the action's.
    private static void followStyleClass(ObservableList<String> target, MessageAction action,
            Map<Object, Object> holder) {
        target.add(STYLE_CLASS);
        List<String> own = List.copyOf(target);
        Runnable sync = () -> {
            List<String> classes = new ArrayList<>(own);
            classes.addAll(action.getStyleClass());
            target.setAll(classes);
        };
        sync.run();
        ListChangeListener<String> update = _ -> sync.run();
        holder.put(LISTENER_KEY, update);
        action.getStyleClass().addListener(new WeakListChangeListener<>(update));
    }
}
