package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ContextMenuEvent;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.MessageAction;

/// The context menu of message text, the same in every layout: *Copy* and *Select All* (what the
/// area's own menu offers a read-only text), then the pane's [MessageAction]s for the message
/// under the pointer.
///
/// Replaces the area's default menu: the area shows its own only while it has neither a context
/// menu nor a context-menu handler.
// [impl->dsn~message-actions~2]
final class MessageMenu {

    private MessageMenu() {
    }

    /// Installs the menu on `area`; `messageAt` tells which message a request points at (`null`:
    /// none, only *Copy* and *Select All*).
    static void install(RichTextArea area, RenderContext context,
            Function<ContextMenuEvent, @Nullable ChatMessage> messageAt) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("message-menu");
        area.setOnContextMenuRequested(event -> {
            menu.getItems().setAll(items(area, context, messageAt.apply(event)));
            menu.show(area, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    /// The items for message (package-private for the UI test).
    static List<MenuItem> items(RichTextArea area, RenderContext context, @Nullable ChatMessage message) {
        MenuItem copy = new MenuItem(context.localize(ChatPane.TEXT_COPY));
        SelectionSegment selection = area.getSelection();
        copy.setDisable(selection == null || selection.isCollapsed());
        copy.setOnAction(_ -> area.copy());
        MenuItem selectAll = new MenuItem(context.localize(ChatPane.TEXT_SELECT_ALL));
        selectAll.setOnAction(_ -> area.selectAll());
        List<MenuItem> items = new ArrayList<>(List.of(copy, selectAll));
        if (message != null) {
            List<MessageAction> actions = context.actionsFor(message);
            if (!actions.isEmpty()) {
                items.add(new SeparatorMenuItem());
            }
            for (MessageAction action : actions) {
                MenuItem item = new MenuItem(action.text());
                var graphic = action.graphic();
                if (graphic != null) {
                    item.setGraphic(graphic.get());
                }
                item.getStyleClass().add("message-action");
                item.setOnAction(_ -> action.onAction().accept(message));
                items.add(item);
            }
        }
        return items;
    }
}
