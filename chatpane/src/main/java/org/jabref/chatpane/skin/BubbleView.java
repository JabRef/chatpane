package org.jabref.chatpane.skin;

import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;

/// The [org.jabref.chatpane.MessageLayout#BUBBLES] view: a virtualized [ListView]
/// (MADR 0009) of [MessageCell]s over its own copy of the messages.
///
/// The mouse wheel over a bubble scrolls the list by itself: a body as tall as its text passes
/// wheel events on. (The `TextArea` bodies before did not; the filter that fixed it, W2 in
/// docs/workarounds.md, went with them.)
// [impl->dsn~bubble-view~2]
final class BubbleView implements ConversationView {

    private final ObservableList<ChatMessage> items = FXCollections.observableArrayList();
    private final ListView<ChatMessage> list = new ListView<>(items);

    BubbleView(RenderContext context) {
        list.getStyleClass().add("chat-pane-list");
        list.setFocusTraversable(false);
        list.setCellFactory(_ -> new MessageCell(context));
    }

    @Override
    public Node node() {
        return list;
    }

    @Override
    public void show(List<ChatMessage> messages) {
        replaced(messages);
    }

    @Override
    public void hide() {
        items.clear();
    }

    @Override
    public void appended(List<ChatMessage> messages, int from) {
        items.addAll(messages.subList(from, messages.size()));
        followEnd();
    }

    /// Sets the one item, so only its cell is built again; its successor's cell too if the change
    /// affects whether that one continues the group.
    @Override
    public void updated(List<ChatMessage> messages, int index) {
        ChatMessage old = items.get(index);
        ChatMessage current = messages.get(index);
        items.set(index, current);
        if (index + 1 < items.size()) {
            ChatMessage next = items.get(index + 1);
            if (MessageGrouping.continuesGroup(old, next) != MessageGrouping.continuesGroup(current, next)) {
                items.set(index + 1, next);
            }
        }
        if (index == items.size() - 1) {
            followEnd();
        }
    }

    @Override
    public void replaced(List<ChatMessage> messages) {
        items.setAll(messages);
        followEnd();
    }

    /// Scrolls to the last message — twice: a new bubble's body knows its final height only after
    /// its RichTextArea laid out, one pulse later, so the first scroll can end a few pixels short.
    /// Builds the visible cells again: each body resolved its styles when it was built (W7).
    @Override
    public void restyle() {
        list.refresh();
    }

    private void followEnd() {
        if (!hasSelection() && !items.isEmpty()) {
            list.scrollTo(items.size() - 1);
            Platform.runLater(() -> {
                if (!hasSelection() && !items.isEmpty()) {
                    list.scrollTo(items.size() - 1);
                }
            });
        }
    }

    /// Text is selected in a bubble: the focused node is one of this list's message bodies, with a
    /// non-empty selection.
    private boolean hasSelection() {
        @Nullable Node focused = list.getScene() == null ? null : list.getScene().getFocusOwner();
        return focused instanceof BubbleText body && isInList(body)
                && body.getSelection() != null && !body.getSelection().isCollapsed();
    }

    private boolean isInList(Node node) {
        for (Node parent = node; parent != null; parent = parent.getParent()) {
            if (parent == list) {
                return true;
            }
        }
        return false;
    }

}
