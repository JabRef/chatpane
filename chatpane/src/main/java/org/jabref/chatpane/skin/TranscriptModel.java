package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;

/// The read-only document behind the transcript [jfx.incubator.scene.control.richtext.RichTextArea],
/// virtual like the list of the bubble view: it holds the messages and, per message, the index of
/// its first paragraph — nothing else. A paragraph is built by the [TranscriptFormat] only
/// when the area asks for it, and the paragraphs of the last [#CACHED_MESSAGES] messages asked
/// for are kept. (Holding every built paragraph cost about 1 KB per message: 100 MB for 100 000.)
///
/// The document grows at the end ([#append(List, int)]) — the common case of a chat — and a
/// message can be replaced in place ([#update(List, int)]) — an answer being generated; both fire
/// the matching change event, so the view keeps its scroll position and the user's selection.
/// Anything else is a new model.
///
/// A document always has at least one paragraph; an empty transcript is one empty paragraph,
/// which the first append replaces.
// [impl->dsn~transcript-model~4]
final class TranscriptModel extends StyledTextModelViewOnlyBase {

    /// Messages whose paragraphs stay built: a screenful, with room to scroll back and forth.
    static final int CACHED_MESSAGES = 256;

    private static final RichParagraph EMPTY = RichParagraph.builder().build();

    private final TranscriptFormat format;
    private final List<ChatMessage> messages = new ArrayList<>();

    /// `starts[i]` is the index of message `i`'s first paragraph; `starts[size]` the paragraph count.
    private int[] starts = new int[16];

    private final Map<Integer, List<TranscriptLine>> built = new LinkedHashMap<>(CACHED_MESSAGES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, List<TranscriptLine>> eldest) {
            return size() > CACHED_MESSAGES;
        }
    };

    TranscriptModel(TranscriptFormat format, List<ChatMessage> initial) {
        this.format = format;
        add(initial, 0);
    }

    /// Adds `all.subList(from, all.size())` at the end of the document; `all` is the whole message
    /// list, whose element before `from` decides whether the first one continues a group.
    void append(List<ChatMessage> all, int from) {
        if (from >= all.size()) {
            return;
        }
        TextPos end = getDocumentEnd();
        boolean wasEmpty = messages.isEmpty();
        int oldCount = paragraphCount();
        add(all, from);
        int added = paragraphCount() - oldCount;
        int lastLength = getPlainText(size() - 1).length();
        if (wasEmpty) {
            // The placeholder paragraph becomes the first added one: its text lands on the
            // existing line, every further paragraph is a line of its own.
            int firstLength = getPlainText(0).length();
            if (added == 1) {
                fireChangeEvent(end, end, firstLength, 0, 0);
            } else {
                fireChangeEvent(end, end, firstLength, added - 1, lastLength);
            }
        } else {
            // Inserting "\n p1 \n … \n pN" at the end: no text on the old last line, N new lines.
            fireChangeEvent(end, end, 0, added, lastLength);
        }
    }

    private void add(List<ChatMessage> all, int from) {
        for (int i = from; i < all.size(); i++) {
            ChatMessage message = all.get(i);
            @Nullable ChatMessage previous = messages.isEmpty() ? null : messages.getLast();
            int count = format.paragraphCount(message, MessageGrouping.continuesGroup(previous, message));
            if (starts.length < messages.size() + 2) {
                starts = Arrays.copyOf(starts, starts.length * 2);
            }
            starts[messages.size() + 1] = starts[messages.size()] + count;
            messages.add(message);
        }
    }

    private int paragraphCount() {
        return starts[messages.size()];
    }

    @Override
    public int size() {
        return messages.isEmpty() ? 1 : paragraphCount();
    }

    @Override
    public String getPlainText(int index) {
        return getParagraph(index).getPlainText();
    }

    @Override
    public RichParagraph getParagraph(int index) {
        if (messages.isEmpty()) {
            return EMPTY;
        }
        return lineAt(index).paragraph();
    }

    /// Replaces the message at `index` by `all.get(index)` and fires the change of its paragraphs,
    /// keeping the rest — the path of an answer that grows while it is generated. Returns `false`,
    /// changing nothing, if the replacement changes whether the next message continues the group:
    /// then the caller rebuilds.
    boolean update(List<ChatMessage> all, int index) {
        ChatMessage old = messages.get(index);
        ChatMessage current = all.get(index);
        if (index + 1 < messages.size()) {
            ChatMessage next = messages.get(index + 1);
            if (MessageGrouping.continuesGroup(old, next) != MessageGrouping.continuesGroup(current, next)) {
                return false;
            }
        }
        int first = starts[index];
        int oldCount = starts[index + 1] - first;
        TextPos start = TextPos.ofLeading(first, 0);
        TextPos end = TextPos.ofLeading(first + oldCount - 1, getPlainText(first + oldCount - 1).length());

        @Nullable ChatMessage previous = index > 0 ? messages.get(index - 1) : null;
        int newCount = format.paragraphCount(current, MessageGrouping.continuesGroup(previous, current));
        messages.set(index, current);
        built.remove(index);
        int delta = newCount - oldCount;
        for (int i = index + 1; i <= messages.size(); i++) {
            starts[i] += delta;
        }
        int firstLength = getPlainText(first).length();
        if (newCount == 1) {
            fireChangeEvent(start, end, firstLength, 0, 0);
        } else {
            fireChangeEvent(start, end, firstLength, newCount - 1, getPlainText(first + newCount - 1).length());
        }
        return true;
    }

    /// The message a paragraph position belongs to (for the context menu), if any.
    @Nullable ChatMessage messageAt(TextPos pos) {
        if (messages.isEmpty() || pos.index() < 0 || pos.index() >= size()) {
            return null;
        }
        return messages.get(messageAt(pos.index()));
    }

    /// The target of the link at `pos`, if there is one (for [LinkInteraction]).
    @Nullable String linkAt(TextPos pos) {
        if (messages.isEmpty() || pos.index() < 0 || pos.index() >= size()) {
            return null;
        }
        return lineAt(pos.index()).linkAt(pos.offset());
    }

    private TranscriptLine lineAt(int index) {
        int message = messageAt(index);
        return paragraphsOf(message).get(index - starts[message]);
    }

    /// The message whose paragraphs include paragraph `index`: the last start at or before it.
    private int messageAt(int index) {
        int found = Arrays.binarySearch(starts, 0, messages.size(), index);
        if (found >= 0) {
            // A message of zero paragraphs shares its start with the next one; take the last.
            while (found + 1 < messages.size() && starts[found + 1] == index) {
                found++;
            }
            return found;
        }
        return -found - 2;
    }

    private List<TranscriptLine> paragraphsOf(int message) {
        return built.computeIfAbsent(message, i -> {
            @Nullable ChatMessage previous = i > 0 ? messages.get(i - 1) : null;
            ChatMessage current = messages.get(i);
            return format.paragraphs(current, MessageGrouping.continuesGroup(previous, current));
        });
    }

    /// Only asked for when inserting or copying with styles; the styles here are CSS names on the
    /// segments, which carry over by themselves, so paragraph attributes are all there is to say.
    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos) {
        if (pos.index() < 0 || pos.index() >= size()) {
            return StyleAttributeMap.EMPTY;
        }
        StyleAttributeMap attributes = getParagraph(pos.index()).getParagraphAttributes();
        return attributes == null ? StyleAttributeMap.EMPTY : attributes;
    }
}
