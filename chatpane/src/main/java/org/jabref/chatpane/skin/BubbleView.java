package org.jabref.chatpane.skin;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.FindMatch;

/// The [org.jabref.chatpane.MessageLayout#BUBBLES] view: a virtualized [ListView]
/// (MADR 0009) of [MessageCell]s over its own copy of the messages, laid out by a [BubbleFlow]
/// that stays at the end while the newest bubble settles its height.
///
/// The mouse wheel over a bubble scrolls the list by itself: a body as tall as its text passes
/// wheel events on. (The `TextArea` bodies before did not; the filter that fixed it, W2 in
/// docs/workarounds.md, went with them.)
// [impl->dsn~bubble-view~3]
final class BubbleView implements ConversationView {

    private final ObservableList<ChatMessage> items = FXCollections.observableArrayList();
    private final ListView<ChatMessage> list = new ListView<>(items);
    private final BubbleFlow.Skin skin = new BubbleFlow.Skin(list);

    BubbleView(RenderContext context) {
        list.getStyleClass().add("chat-pane-list");
        list.setFocusTraversable(false);
        list.setCellFactory(_ -> new MessageCell(context));
        list.setSkin(skin);
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

    /// Builds the visible cells again: each body resolved its styles when it was built (W7).
    @Override
    public void restyle() {
        list.refresh();
    }

    /// Every bubble body builds its paragraphs, highlights included, when its cell is built.
    // [impl->dsn~find-highlights~1]
    @Override
    public void findChanged() {
        list.refresh();
    }

    /// Scrolls the match's bubble to the top of the list.
    // [impl->dsn~find-reveal~1]
    @Override
    public void reveal(FindMatch match) {
        if (match.message() < items.size()) {
            list.scrollTo(match.message());
        }
    }

    /// Scrolls to the last message and keeps the flow there while its bubble settles its height
    /// ([BubbleFlow]); with text selected, the view stays where it is.
    private void followEnd() {
        if (hasSelection()) {
            skin.flow().stay();
        } else if (!items.isEmpty()) {
            list.scrollTo(items.size() - 1);
            skin.flow().follow();
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
