package org.jabref.chatpane.skin;

import java.util.List;

import javafx.scene.Node;

import org.jabref.chatpane.ChatMessage;

/// One way of showing the conversation inside [ChatPaneSkin]: the bubble list ([BubbleView]) or
/// a transcript ([TranscriptView]).
///
/// The skin keeps one view per layout and shows one at a time. Only the shown view tracks the
/// messages: [#show(List)] builds it from the current messages, [#hide()] lets go of them, and in
/// between the skin reports each change as [#appended(List, int)], [#updated(List, int)] or
/// [#replaced(List)].
///
/// Every view follows the newest message the same way: after a change it scrolls to the end,
/// unless the user has text selected in it — a selection is never lost to an incoming message.
// [impl->dsn~conversation-views~3]
interface ConversationView {

    /// The node the skin puts on screen while this view is shown.
    Node node();

    /// Starts showing `messages`.
    void show(List<ChatMessage> messages);

    /// Stops showing and releases the messages, so a hidden view keeps nothing alive.
    void hide();

    /// Messages were added at the end of `messages`, the first of them at index `from`.
    void appended(List<ChatMessage> messages, int from);

    /// The message at `index` was replaced by another — typically the same message grown or with
    /// a new status; the view updates it in place (or re-renders, if it cannot).
    void updated(List<ChatMessage> messages, int index);

    /// The styles the text is drawn with changed (theme, stylesheet, font): draw it again.
    void restyle();

    /// Any other change: `messages` is the whole new list.
    void replaced(List<ChatMessage> messages);
}
