package org.jabref.chatpane.demo;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatMessage.Status;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;

/// A pretend assistant, the way an AI chat (JabRef's) answers: a pending message that grows word by
/// word — each step replaces the message in the list, which the pane updates in place — and ends
/// sent, or failed if the question contains "fail".
// [impl->dsn~demo-app~3]
final class DemoResponder {

    static final String NAME = "assistant";

    private static final String ANSWER = "Here is what I found: **ChatPane** updates a growing answer *in place*, "
            + "so your selection and the scroll position stay.\n- it works in every layout\n- try `fail` for an error";

    private final ObservableList<ChatMessage> messages;

    DemoResponder(ObservableList<ChatMessage> messages) {
        this.messages = messages;
    }

    /// Answers `question` in a new message at the end.
    void answer(String question) {
        ChatMessage pending = new ChatMessage(NAME, "…", Instant.now(), INCOMING, Status.PENDING);
        messages.add(pending);
        stream(pending, question.toLowerCase(Locale.ROOT).contains("fail"));
    }

    /// Answers again in place of `failed`.
    void retry(ChatMessage failed) {
        ChatMessage pending = failed.withText("…").withStatus(Status.PENDING);
        replace(failed, pending);
        stream(pending, false);
    }

    private void stream(ChatMessage start, boolean fail) {
        List<String> words = List.of(ANSWER.split("(?<= )"));
        ChatMessage[] current = {start};
        Timeline timeline = new Timeline();
        for (int i = 1; i <= words.size(); i++) {
            String text = String.join("", words.subList(0, i));
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(60.0 * i), _ -> current[0] = replace(current[0], current[0].withText(text))));
        }
        timeline.setOnFinished(_ -> replace(current[0], fail
                ? current[0].withText("Could not answer: the demo fails on purpose. Use *Retry*.").withStatus(Status.ERROR)
                : current[0].withStatus(Status.SENT)));
        timeline.play();
    }

    /// Replaces `old` — found by identity, as records compare by value — and returns the new one.
    private ChatMessage replace(ChatMessage old, ChatMessage replacement) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) == old) {
                messages.set(i, replacement);
                return replacement;
            }
        }
        return old;
    }
}
