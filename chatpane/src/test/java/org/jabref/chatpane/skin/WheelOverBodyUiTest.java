package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

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

/// The mouse wheel over a bubble's text scrolls the conversation. With `TextArea` bodies it did
/// not (their inner ScrollPane consumed every wheel event; W2 in docs/workarounds.md, removed): this test
/// passes without any filter since the bodies are content-high RichTextAreas.
// [utest->dsn~bubble-view~2]
@Tag("ui")
@TestFxApplication(WheelOverBodyUiTest.TestApp.class)
class WheelOverBodyUiTest {

    private static ChatPane pane;

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            pane.setMessageLayout(MessageLayout.BUBBLES);
            pane.getMessages().setAll(IntStream.range(0, 40)
                    .mapToObj(i -> new ChatMessage("alice", "Message " + i, Instant.parse("2026-09-22T10:00:00Z").plusSeconds(600L * i), INCOMING))
                    .toList());
            stage.setScene(new Scene(pane, 400, 300));
            stage.show();
        }
    }

    @Test
    void wheelOverABodyScrollsTheList() throws Exception {
        settle(pane);
        int before = onFx(() -> {
            VirtualFlow<?> flow = (VirtualFlow<?>) pane.lookup(".virtual-flow");
            flow.scrollTo(0);
            pane.layout();
            return flow.getFirstVisibleCell().getIndex();
        });
        settle(pane);
        onFx(() -> {
            BubbleText body = (BubbleText) pane.lookup(".message-body");
            // The deepest node under the mouse, as a real wheel event targets it: a shown Text.
            Node text = body.lookupAll("Text").stream()
                    .filter(node -> node instanceof Text shown && shown.getText().startsWith("Message"))
                    .findFirst().orElseThrow();
            Bounds bounds = text.localToScreen(text.getBoundsInLocal());
            ScrollEvent wheel = new ScrollEvent(ScrollEvent.SCROLL, 5, 5, bounds.getCenterX(), bounds.getCenterY(),
                    false, false, false, false, false, false, 0, -120, 0, -120,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0,
                    new PickResult(text, bounds.getCenterX(), bounds.getCenterY()));
            text.fireEvent(wheel);
            return null;
        });
        settle(pane);
        int after = onFx(() -> ((VirtualFlow<?>) pane.lookup(".virtual-flow")).getFirstVisibleCell().getIndex());
        assertThat(after).as("first visible bubble after one wheel notch down").isGreaterThan(before);
    }
}
