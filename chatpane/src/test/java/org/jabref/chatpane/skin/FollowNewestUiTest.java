package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.skin.VirtualFlow;
import javafx.stage.Stage;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;

import io.gitlab.fxlabs.testfx.junit.jupiter.TestFxApplication;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.MessageLayout;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;

/// Both views follow the newest message by the same rule: to the end after a change, unless the
/// user has text selected — the transcript kept a selection from the start, the bubbles jumped to
/// the end regardless until the views were split (review finding S2).
// [utest->dsn~conversation-views~3]
// [utest->dsn~bubble-view~2]
@Tag("ui")
@TestFxApplication(FollowNewestUiTest.TestApp.class)
class FollowNewestUiTest {

    private static final Instant T0 = Instant.parse("2026-09-22T10:00:00Z");

    private static ChatPane pane;

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            stage.setScene(new Scene(pane, 400, 300));
            stage.show();
        }
    }

    /// Forty messages, far more than fit, in `layout`; laid out and scrolled to the end.
    private static void fill(MessageLayout layout) throws Exception {
        onFx(() -> {
            pane.setMessageLayout(layout);
            pane.getMessages().setAll(IntStream.range(0, 40)
                    .mapToObj(i -> new ChatMessage("alice", "Message " + i, T0.plusSeconds(600L * i), INCOMING))
                    .toList());
            pane.applyCss();
            pane.layout();
            return null;
        });
    }

    private static void append() throws Exception {
        onFx(() -> {
            pane.getMessages().add(new ChatMessage("bob", "The newest", T0.plusSeconds(600L * 41), INCOMING));
            pane.applyCss();
            pane.layout();
            return null;
        });
    }

    private static int lastVisibleBubble() throws Exception {
        settle(pane);
        return onFx(() -> ((VirtualFlow<?>) pane.lookup(".virtual-flow")).getLastVisibleCell().getIndex());
    }

    @Test
    void bubblesFollowWithoutSelection() throws Exception {
        fill(MessageLayout.BUBBLES);
        append();
        assertThat(lastVisibleBubble()).isEqualTo(40);
    }

    @Test
    void bubblesStayWhileTextIsSelected() throws Exception {
        fill(MessageLayout.BUBBLES);
        onFx(() -> {
            VirtualFlow<?> flow = (VirtualFlow<?>) pane.lookup(".virtual-flow");
            flow.scrollTo(5);
            pane.layout();
            BubbleText body = (BubbleText) flow.getCell(5).lookup(".message-body");
            body.requestFocus();
            body.selectAll();
            return null;
        });
        int before = lastVisibleBubble();
        append();
        assertThat(lastVisibleBubble()).isEqualTo(before);
    }

    @Test
    void transcriptFollowsWithoutSelectionAndStaysWithOne() throws Exception {
        fill(MessageLayout.IRC);
        RichTextArea transcript = onFx(() -> (RichTextArea) pane.lookup(".chat-pane-transcript"));
        append();
        assertThat(onFx(() -> transcript.getCaretPosition().index())).as("caret at the new end").isEqualTo(40);

        onFx(() -> {
            transcript.select(TextPos.ofLeading(3, 0), TextPos.ofLeading(3, 4));
            return null;
        });
        append();
        assertThat(onFx(() -> transcript.getSelection().getMin().index())).as("selection kept").isEqualTo(3);
    }
}
