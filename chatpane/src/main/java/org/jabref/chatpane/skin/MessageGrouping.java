package org.jabref.chatpane.skin;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;

/// Decides which messages form a group, so a layout can drop the repeated header.
// [impl->dsn~message-grouping~1]
final class MessageGrouping {

    /// Longest gap after which a message by the same sender still continues the group.
    static final Duration MAX_GAP = Duration.ofMinutes(5);

    private MessageGrouping() {
    }

    /// Whether `current` continues the group `previous` belongs to: same sender, same direction,
    /// and sent no earlier than `previous` and at most [#MAX_GAP] after it.
    ///
    /// A message listed before an older one starts a new group: the list order is the caller's,
    /// and a header is the honest way to show a jump back in time.
    static boolean continuesGroup(@Nullable ChatMessage previous, ChatMessage current) {
        if (previous == null
                || previous.direction() != current.direction()
                || !previous.sender().equals(current.sender())) {
            return false;
        }
        Duration gap = Duration.between(previous.sentAt(), current.sentAt());
        return !gap.isNegative() && gap.compareTo(MAX_GAP) <= 0;
    }
}
