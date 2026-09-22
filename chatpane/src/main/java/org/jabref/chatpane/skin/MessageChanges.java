package org.jabref.chatpane.skin;

import javafx.collections.ListChangeListener;

/// Classifies a change of the message list, so a view can append instead of rebuilding.
// [impl->dsn~message-changes~2]
final class MessageChanges {

    /// Returned by [#appendedFrom] and [#replacedAt] when the change is not of their kind.
    static final int NOT_AN_APPEND = -1;

    private MessageChanges() {
    }

    /// The index of the one message `change` replaced by another (`set(index, …)`, the way an
    /// answer being generated grows), else [#NOT_AN_APPEND]. Leaves the change reset.
    static int replacedAt(ListChangeListener.Change<?> change) {
        int index = NOT_AN_APPEND;
        try {
            if (change.next() && change.wasReplaced() && change.getRemovedSize() == 1 && change.getAddedSize() == 1
                    && !change.next()) {
                index = changeFrom(change);
            }
        } finally {
            change.reset();
        }
        return index;
    }

    private static int changeFrom(ListChangeListener.Change<?> change) {
        change.reset();
        change.next();
        return change.getFrom();
    }

    /// The index of the first added message if `change` only added messages at the end of the
    /// list, else [#NOT_AN_APPEND]. Leaves the change reset, so other listeners can read it too.
    static int appendedFrom(ListChangeListener.Change<?> change) {
        int oldSize = change.getList().size();
        int from = NOT_AN_APPEND;
        try {
            while (change.next()) {
                if (!change.wasAdded() || change.wasRemoved() || change.wasPermutated() || change.wasUpdated()) {
                    return NOT_AN_APPEND;
                }
                oldSize -= change.getAddedSize();
                from = from == NOT_AN_APPEND ? change.getFrom() : Math.min(from, change.getFrom());
            }
        } finally {
            change.reset();
        }
        return from != NOT_AN_APPEND && from == oldSize ? from : NOT_AN_APPEND;
    }
}
