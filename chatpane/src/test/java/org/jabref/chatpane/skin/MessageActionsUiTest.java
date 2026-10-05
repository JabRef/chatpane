package org.jabref.chatpane.skin;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatMessage.Status;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.FxTestApplication;
import org.jabref.chatpane.MessageAction;
import org.jabref.chatpane.MessageLayout;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.ChatMessage.Direction.OUTGOING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;

/// Message actions (context menu and hover buttons in every layout, all following the action's
/// properties), status, and a message replaced in place as a generated answer grows.
// [utest->dsn~message-actions~4]
// [utest->dsn~conversation-views~4]
@FxTestApplication(MessageActionsUiTest.TestApp.class)
class MessageActionsUiTest {

    private static final Instant T0 = Instant.parse("2026-09-22T10:00:00Z");

    private static ChatPane pane;
    private static final List<ChatMessage> acted = new ArrayList<>();
    private static final MessageAction delete = new MessageAction("Delete", acted::add);
    private static final MessageAction retry = new MessageAction("Retry", m -> m.status() == Status.ERROR, acted::add);

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            pane.getMessageActions().addAll(delete, retry);
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
            for (MessageAction action : List.of(delete, retry)) {
                action.setDisable(false);
                action.setVisible(true);
                action.setGraphic(null);
                action.getStyleClass().clear();
            }
            delete.setText("Delete");
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
        Button button = onFx(() -> (Button) pane.lookup(".message-action"));
        assertThat(onFx(button::getText)).isEqualTo("Delete");
        assertThat(onFx(() -> pane.lookupAll(".message-action").size())).as("Retry only for errors").isEqualTo(1);
        onFx(() -> {
            button.fire();
            return null;
        });
        assertThat(acted).containsExactly(hello);
    }

    @Test
    void transcriptShowsActionButtonsRightOfTheMessageUnderThePointer() throws Exception {
        ChatMessage failed = new ChatMessage("me", "Did not go through", T0.plusSeconds(600), OUTGOING, Status.ERROR);
        for (MessageLayout layout : List.of(MessageLayout.IRC, MessageLayout.MODERN)) {
            show(layout, new ChatMessage("alice", "Hi", T0, INCOMING), failed);
            RichTextArea area = onFx(() -> (RichTextArea) pane.lookup(".chat-pane-transcript"));
            assertThat(onFx(() -> visibleActions(area))).as("%s: none before the pointer comes", layout).isEmpty();

            Bounds text = onFx(() -> shownText(area, "Did not go through").localToScreen(
                    shownText(area, "Did not go through").getBoundsInLocal()));
            pointer(area, MouseEvent.MOUSE_MOVED, text.getCenterX(), text.getCenterY());
            List<Button> buttons = onFx(() -> visibleActions(area));
            assertThat(onFx(() -> buttons.stream().map(Button::getText).toList())).as(layout.name())
                    .containsExactly("Delete", "Retry");
            Bounds first = onFx(() -> buttons.getFirst().localToScreen(buttons.getFirst().getBoundsInLocal()));
            assertThat(first.getMinX()).as("%s: right of the text", layout).isGreaterThanOrEqualTo(text.getMaxX());
            assertThat(first.getMinY()).as("%s: not below the message", layout).isLessThan(text.getMaxY());

            onFx(() -> {
                buttons.getLast().fire();
                return null;
            });
            assertThat(acted).as(layout.name()).containsExactly(failed);

            pointer(area, MouseEvent.MOUSE_EXITED, text.getCenterX(), text.getCenterY());
            assertThat(onFx(() -> visibleActions(area))).as("%s: gone with the pointer", layout).isEmpty();
        }
    }

    @Test
    void transcriptKeepsNoButtonColumnWithoutActions() throws Exception {
        show(MessageLayout.IRC, new ChatMessage("alice", "Hi", T0, INCOMING));
        RichTextArea area = onFx(() -> (RichTextArea) pane.lookup(".chat-pane-transcript"));
        assertThat(onFx(area::getRightDecorator)).isNotNull();
        List<MessageAction> actions = onFx(() -> List.copyOf(pane.getMessageActions()));
        try {
            onFx(() -> {
                pane.getMessageActions().clear();
                return null;
            });
            assertThat(onFx(area::getRightDecorator)).isNull();
        } finally {
            onFx(() -> pane.getMessageActions().setAll(actions));
        }
    }

    @Test
    void buttonsFollowTheActionWithoutARerender() throws Exception {
        show(MessageLayout.BUBBLES, new ChatMessage("alice", "Hi", T0, INCOMING));
        Button button = onFx(() -> (Button) pane.lookup(".message-action"));
        onFx(() -> {
            delete.setText("Remove");
            delete.setDisable(true);
            delete.getStyleClass().add("delete");
            delete.setGraphic(() -> new Rectangle(8, 8));
            return null;
        });
        assertThat(onFx(() -> pane.lookup(".message-action"))).as("same button, updated in place").isSameAs(button);
        assertThat(onFx(button::getText)).as("text kept for screen readers").isEqualTo("Remove");
        assertThat(onFx(button::isDisabled)).isTrue();
        assertThat(onFx(button::getStyleClass)).contains("button", "message-action", "delete");
        assertThat(onFx(button::getGraphic)).isInstanceOf(Rectangle.class);
        assertThat(onFx(button::getContentDisplay)).isEqualTo(ContentDisplay.GRAPHIC_ONLY);
        assertThat(onFx(() -> button.getTooltip().getText())).isEqualTo("Remove");
        onFx(() -> {
            delete.setGraphic(null);
            delete.setVisible(false);
            return null;
        });
        assertThat(onFx(button::getContentDisplay)).isEqualTo(ContentDisplay.TEXT_ONLY);
        assertThat(onFx(button::getTooltip)).isNull();
        assertThat(onFx(() -> button.isVisible() || button.isManaged())).as("hidden without a gap").isFalse();
    }

    @Test
    void menuItemsFollowTheAction() throws Exception {
        ChatMessage failed = new ChatMessage("me", "Did not go through", T0, OUTGOING, Status.ERROR);
        show(MessageLayout.IRC, failed);
        List<MenuItem> items = onFx(() -> MessageMenu.items((RichTextArea) pane.lookup(".chat-pane-transcript"),
                RenderContext.of(pane), failed));
        MenuItem separator = items.get(2);
        MenuItem retryItem = items.get(4);
        onFx(() -> {
            retry.setText("Try again");
            retry.setDisable(true);
            retry.getStyleClass().add("retry");
            delete.setVisible(false);
            return null;
        });
        assertThat(onFx(retryItem::getText)).isEqualTo("Try again");
        assertThat(onFx(retryItem::isDisable)).isTrue();
        assertThat(onFx(retryItem::getStyleClass)).contains("message-action", "retry");
        assertThat(onFx(() -> items.get(3).isVisible())).isFalse();
        assertThat(onFx(separator::isVisible)).as("a visible action is left").isTrue();
        onFx(() -> {
            retry.setText("Retry");
            retry.setVisible(false);
            return null;
        });
        assertThat(onFx(separator::isVisible)).as("no visible action left").isFalse();
    }

    @Test
    void anActionKeepsNoControlAlive() throws Exception {
        ChatMessage hello = new ChatMessage("alice", "Hi", T0, INCOMING);
        WeakReference<Button> button = onFx(() -> new WeakReference<>(ActionControls.button(delete, () -> delete.getOnAction().accept(hello))));
        WeakReference<MenuItem> item = onFx(() -> new WeakReference<>(ActionControls.menuItem(delete, hello)));
        for (int i = 0; i < 50 && (button.get() != null || item.get() != null); i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertThat(button.get()).as("button").isNull();
        assertThat(item.get()).as("menu item").isNull();
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

    private static void pointer(RichTextArea area, javafx.event.EventType<MouseEvent> type, double screenX, double screenY)
            throws Exception {
        onFx(() -> {
            javafx.geometry.Point2D local = area.screenToLocal(screenX, screenY);
            area.fireEvent(new MouseEvent(type, local.getX(), local.getY(), screenX, screenY, MouseButton.NONE, 0,
                    false, false, false, false, false, false, false, false, false, false, null));
            return null;
        });
        settle(pane);
    }

    private static List<Button> visibleActions(RichTextArea area) {
        return area.lookupAll(".message-action").stream()
                .filter(node -> node.getScene() != null && node.isVisible())
                .map(node -> (Button) node)
                .toList();
    }

    private static Text shownText(RichTextArea area, String content) {
        return area.lookupAll("Text").stream()
                .filter(node -> node instanceof Text text && content.equals(text.getText()))
                .map(node -> (Text) node)
                .findFirst().orElseThrow();
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
