package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

import io.gitlab.fxlabs.testfx.junit.jupiter.TestFxApplication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatMessage.Status;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.MessageAction;
import org.jabref.chatpane.MessageLayout;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.ChatMessage.Direction.OUTGOING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;

/// Message actions (context menu in every layout, hover buttons on bubbles), status, and a message
/// replaced in place as a generated answer grows.
// [utest->dsn~message-actions~2]
// [utest->dsn~conversation-views~3]
@Tag("ui")
@TestFxApplication(MessageActionsUiTest.TestApp.class)
class MessageActionsUiTest {

    private static final Instant T0 = Instant.parse("2026-09-22T10:00:00Z");

    private static ChatPane pane;
    private static final List<ChatMessage> acted = new ArrayList<>();

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            pane.getMessageActions().addAll(
                    MessageAction.of("Delete", acted::add),
                    MessageAction.of("Retry", acted::add).onlyFor(m -> m.status() == Status.ERROR));
            stage.setScene(new Scene(pane, 480, 360));
            stage.show();
        }
    }

    private static void show(MessageLayout layout, ChatMessage... messages) throws Exception {
        onFx(() -> {
            acted.clear();
            pane.setMessageLayout(layout);
            pane.getMessages().setAll(messages);
            return null;
        });
        settle(pane);
    }

    @AfterEach
    void clear() throws Exception {
        onFx(() -> {
            pane.getMessages().clear();
            return null;
        });
    }

    @Test
    void contextMenuOffersCopyAndTheActionsThatApply() throws Exception {
        ChatMessage failed = new ChatMessage("me", "Did not go through", T0, OUTGOING, Status.ERROR);
        for (MessageLayout layout : MessageLayout.values()) {
            show(layout, new ChatMessage("alice", "Hi", T0, INCOMING), failed);
            List<String> texts = onFx(() -> {
                RichTextArea area = layout == MessageLayout.BUBBLES
                        ? lastBody()
                        : (RichTextArea) pane.lookup(".chat-pane-transcript");
                ChatMessage target = layout == MessageLayout.BUBBLES ? failed
                        : ((TranscriptModel) area.getModel()).messageAt(TextPos.ofLeading(area.getModel().size() - 1, 0));
                List<MenuItem> items = MessageMenu.items(area, RenderContext.of(pane), target);
                items.getLast().fire();
                return items.stream().map(item -> item.getText() == null ? "---" : item.getText()).toList();
            });
            assertThat(texts).as(layout.name()).containsExactly("Copy", "Select All", "---", "Delete", "Retry");
            assertThat(acted).as(layout.name()).containsExactly(failed);
        }
    }

    @Test
    void menuTextsGoThroughTheLocalizer() throws Exception {
        show(MessageLayout.IRC, new ChatMessage("alice", "Hi", T0, INCOMING));
        onFx(() -> {
            pane.setTextLocalizer(english -> switch (english) {
                case ChatPane.TEXT_COPY -> "Kopieren";
                case ChatPane.TEXT_SELECT_ALL -> "Alles auswählen";
                default -> english;
            });
            return null;
        });
        try {
            List<String> texts = onFx(() -> MessageMenu.items((RichTextArea) pane.lookup(".chat-pane-transcript"),
                    RenderContext.of(pane), null).stream().map(MenuItem::getText).toList());
            assertThat(texts).containsExactly("Kopieren", "Alles auswählen");
        } finally {
            onFx(() -> {
                pane.setTextLocalizer(null);
                return null;
            });
        }
    }

    @Test
    void bubblesShowActionButtonsOnTheInnerSide() throws Exception {
        ChatMessage hello = new ChatMessage("alice", "Hi", T0, INCOMING);
        show(MessageLayout.BUBBLES, hello);
        Button delete = onFx(() -> (Button) pane.lookup(".message-action"));
        assertThat(onFx(delete::getText)).isEqualTo("Delete");
        assertThat(onFx(() -> pane.lookupAll(".message-action").size())).as("Retry only for errors").isEqualTo(1);
        onFx(() -> {
            delete.fire();
            return null;
        });
        assertThat(acted).containsExactly(hello);
    }

    @Test
    void statusIsAPseudoClassOfTheBubbleCell() throws Exception {
        show(MessageLayout.BUBBLES, new ChatMessage("ai", "Thinking", T0, INCOMING, Status.PENDING));
        assertThat(onFx(() -> cellStates())).contains(PseudoClass.getPseudoClass("pending"))
                .doesNotContain(PseudoClass.getPseudoClass("sent"));
    }

    @Test
    void aGrowingAnswerUpdatesInPlace() throws Exception {
        for (MessageLayout layout : MessageLayout.values()) {
            List<ChatMessage> history = IntStream.range(0, 5)
                    .mapToObj(i -> new ChatMessage("alice", "Message " + i, T0.plusSeconds(600L * i), INCOMING))
                    .toList();
            show(layout, history.toArray(ChatMessage[]::new));
            onFx(() -> {
                pane.getMessages().add(new ChatMessage("ai", "The", T0.plusSeconds(4000), INCOMING, Status.PENDING));
                return null;
            });
            RichTextArea transcript = onFx(() -> (RichTextArea) pane.lookup(".chat-pane-transcript"));
            StyledTextModel before = transcript == null ? null : onFx(transcript::getModel);
            if (transcript != null) {
                onFx(() -> {
                    transcript.select(TextPos.ofLeading(0, 0), TextPos.ofLeading(0, 3));
                    return null;
                });
            }
            for (String text : List.of("The answer", "The answer\ngrows over lines")) {
                onFx(() -> {
                    int last = pane.getMessages().size() - 1;
                    pane.getMessages().set(last, pane.getMessages().get(last).withText(text));
                    return null;
                });
            }
            onFx(() -> {
                int last = pane.getMessages().size() - 1;
                pane.getMessages().set(last, pane.getMessages().get(last).withStatus(Status.SENT));
                return null;
            });
            settle(pane);
            if (transcript != null) {
                assertThat(onFx(transcript::getModel)).as("%s: same model, updated in place", layout).isSameAs(before);
                List<String> lines = onFx(() -> {
                    StyledTextModel model = transcript.getModel();
                    return IntStream.range(0, model.size()).mapToObj(model::getPlainText).toList();
                });
                assertThat(lines.getLast()).isEqualTo("grows over lines");
                assertThat(onFx(() -> transcript.getSelection().getMax())).as("selection kept").isEqualTo(TextPos.ofLeading(0, 3));
                onFx(() -> {
                    transcript.clearSelection();
                    return null;
                });
            } else {
                assertThat(onFx(() -> lastBody().getModel().getPlainText(1))).isEqualTo("grows over lines");
            }
        }
    }

    private static BubbleText lastBody() {
        return pane.lookupAll(".message-body").stream()
                .map(node -> (BubbleText) node)
                .filter(body -> body.getScene() != null)
                .max(java.util.Comparator.comparingDouble(body -> body.localToScene(0, 0).getY()))
                .orElseThrow();
    }

    private static java.util.Set<PseudoClass> cellStates() {
        Node cell = pane.lookupAll(".message-cell").stream()
                .filter(node -> !((ListCell<?>) node).isEmpty())
                .findFirst().orElseThrow();
        return cell.getPseudoClassStates();
    }
}
