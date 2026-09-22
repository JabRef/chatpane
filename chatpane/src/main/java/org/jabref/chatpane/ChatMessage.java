package org.jabref.chatpane;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/// One message shown in a [ChatPane].
///
/// A message is a value: to change one — an answer that grows while it is generated, a failed
/// send — replace it in [ChatPane#getMessages()] (`set(index, message.withText(…))`); the pane
/// updates that message in place.
///
/// @param sender    display name of the author
/// @param text      the message body, as the pane's [MessageRenderer] reads it
/// @param sentAt    when the message was sent
/// @param direction whether the local user wrote it ([Direction#OUTGOING]) or someone else did;
///                  [MessageLayout#BUBBLES] puts outgoing messages on the right
/// @param status    [Status#SENT], or still [Status#PENDING], or [Status#ERROR]
// [impl->dsn~chat-message-model~3]
public record ChatMessage(String sender, String text, Instant sentAt, Direction direction, Status status) {

    /// Who a message comes from, seen from the local user.
    /// The pane does not know user identities; the application says which messages are its own.
    public enum Direction {

        /// Written by someone else.
        INCOMING,

        /// Written by the local user.
        OUTGOING;

        /// The CSS name of this direction — a pseudo-class on a bubble cell, a style name on a
        /// transcript segment: `incoming` or `outgoing`.
        public String cssName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /// Where a message stands. The pane only shows it; what it means is the application's.
    public enum Status {

        /// Delivered, complete: the normal case.
        SENT,

        /// Not there yet — being sent, or an answer still being generated.
        PENDING,

        /// Failed — could not be sent, or the answer is an error.
        ERROR;

        /// The CSS name of this status — a pseudo-class on a bubble cell, a style name on a
        /// transcript segment: `sent`, `pending` or `error`.
        public String cssName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public ChatMessage {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(sentAt, "sentAt");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(status, "status");
    }

    /// A [Status#SENT] message.
    public ChatMessage(String sender, String text, Instant sentAt, Direction direction) {
        this(sender, text, sentAt, direction, Status.SENT);
    }

    /// This message with another text, e.g. the next chunk of a generated answer.
    public ChatMessage withText(String newText) {
        return new ChatMessage(sender, newText, sentAt, direction, status);
    }

    /// This message with another status.
    public ChatMessage withStatus(Status newStatus) {
        return new ChatMessage(sender, text, sentAt, direction, newStatus);
    }
}
