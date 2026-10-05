package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.skin.VirtualFlow;
import javafx.stage.Stage;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.FxTestApplication;
import org.jabref.chatpane.MessageLayout;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/// Both views follow the newest message by the same rule: to the end after a change, unless the
/// user has text selected — the transcript kept a selection from the start, the bubbles jumped to
/// the end regardless until the views were split (review finding S2).
// [utest->dsn~conversation-views~4]
// [utest->dsn~bubble-view~3]
@FxTestApplication(FollowNewestUiTest.TestApp.class)
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

    /// How far the newest bubble's bottom lies below the list's bottom edge: 0 when it is shown whole
    /// at the end, positive when it is cut off.
    private static double newestBubbleCutOff() throws Exception {
        settle(pane);
        return onFx(() -> {
            VirtualFlow<?> flow = (VirtualFlow<?>) pane.lookup(".virtual-flow");
            var newest = flow.getLastVisibleCell();
            assertThat(newest.getIndex()).as("the newest message is the last visible").isEqualTo(pane.getMessages().size() - 1);
            return newest.localToScene(newest.getLayoutBounds()).getMaxY() - flow.localToScene(flow.getLayoutBounds()).getMaxY();
        });
    }

    /// A new bubble knows its height only once its text area laid out, a pulse after the list
    /// scrolled to it; the list kept its scroll fraction and cut the bubble off — and a growing answer
    /// flickered between whole and cut off with every word.
    @Test
    void bubblesShowTheWholeNewestMessageWhileItGrows() throws Exception {
        fill(MessageLayout.BUBBLES);
        String text = "";
        onFx(() -> pane.getMessages().add(new ChatMessage("ai", "…", T0.plusSeconds(600L * 41), INCOMING, ChatMessage.Status.PENDING)));
        assertThat(newestBubbleCutOff()).as("appended").isCloseTo(0, within(1.0));
        for (int word = 0; word < 30; word++) {
            text += "word" + word + " ";
            String grown = text;
            onFx(() -> {
                int last = pane.getMessages().size() - 1;
                pane.getMessages().set(last, pane.getMessages().get(last).withText(grown));
                return null;
            });
            assertThat(newestBubbleCutOff()).as("after %s words", word + 1).isCloseTo(0, within(1.0));
        }
    }

    /// Pinned to the end only until the user scrolls away: a later layout pass leaves the list where
    /// the user put it; scrolling back to the very end pins it again.
    @Test
    void bubblesStayWhereTheUserScrolled() throws Exception {
        fill(MessageLayout.BUBBLES);
        settle(pane);
        double scrolled = onFx(() -> {
            VirtualFlow<?> flow = (VirtualFlow<?>) pane.lookup(".virtual-flow");
            flow.scrollPixels(-200);
            return flow.getPosition();
        });
        onFx(() -> {
            pane.getScene().getWindow().setWidth(pane.getScene().getWindow().getWidth() - 40);
            return null;
        });
        settle(pane);
        assertThat(onFx(() -> ((VirtualFlow<?>) pane.lookup(".virtual-flow")).getPosition()))
                .as("not pulled back to the end").isLessThan(0.99).isCloseTo(scrolled, within(0.05));

        onFx(() -> {
            VirtualFlow<?> flow = (VirtualFlow<?>) pane.lookup(".virtual-flow");
            flow.scrollPixels(10_000);
            pane.getScene().getWindow().setWidth(pane.getScene().getWindow().getWidth() + 40);
            return null;
        });
        assertThat(newestBubbleCutOff()).as("pinned again at the end").isCloseTo(0, within(1.0));
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
