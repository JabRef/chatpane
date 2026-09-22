package org.jabref.chatpane;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

import io.gitlab.fxlabs.testfx.junit.jupiter.TestFxApplication;

import org.jabref.chatpane.skin.ChatPaneSkin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.ChatMessage.Direction.OUTGOING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.assertj.core.api.Assertions.assertThat;

/// Switching the layout re-renders the same messages in the new shape. Needs the toolkit and a
/// window (cells exist only once laid out): `gradlew uiTest` on a desktop, `just uitest` headless.
// [utest->dsn~chat-pane-control~5]
// [utest->dsn~chat-pane-skin~7]
// [utest->dsn~message-cell-bubbles~3]
// [utest->dsn~transcript-view~5]
@Tag("ui")
@TestFxApplication(ChatPaneUiTest.TestApp.class)
class ChatPaneUiTest {

    private static ChatPane pane;

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            Instant t0 = Instant.parse("2026-09-22T10:00:00Z");
            pane.getMessages().addAll(List.of(
                    new ChatMessage("alice", "Hi!", t0, INCOMING),
                    new ChatMessage("alice", "Are you there?", t0.plusSeconds(20), INCOMING),
                    new ChatMessage("me", "Yes.", t0.plusSeconds(60), OUTGOING)));
            stage.setScene(new Scene(pane, 400, 300));
            stage.show();
        }
    }

    @Test
    void newPaneStartsInTheDefaultLayout() throws Exception {
        assertThat(onFx(() -> new ChatPane().getPseudoClassStates())).contains(PseudoClass.getPseudoClass("modern"));
    }

    @Test
    void layoutIsSettableFromCss() throws Exception {
        MessageLayout styled = onFx(() -> {
            ChatPane styledPane = new ChatPane();
            styledPane.setStyle("-cp-message-layout: irc;");
            new Scene(styledPane);
            styledPane.applyCss();
            return styledPane.getMessageLayout();
        });
        assertThat(styled).isEqualTo(MessageLayout.IRC);
    }

    @Test
    void bubblesPutOwnMessagesOnTheOtherSide() throws Exception {
        switchTo(MessageLayout.BUBBLES);
        assertThat(onFx(() -> pane.getPseudoClassStates()))
                .contains(PseudoClass.getPseudoClass("bubbles"))
                .doesNotContain(PseudoClass.getPseudoClass("modern"));
        // Bubble alignment follows the cell's :outgoing state, which CSS styles too.
        List<Pos> alignments = onFx(() -> filledCells().stream()
                .map(cell -> ((HBox) cell.getGraphic()).getAlignment())
                .toList());
        assertThat(alignments).containsExactly(Pos.CENTER_LEFT, Pos.CENTER_LEFT, Pos.CENTER_RIGHT);
        List<Boolean> outgoing = onFx(() -> filledCells().stream()
                .map(cell -> cell.getPseudoClassStates().contains(PseudoClass.getPseudoClass("outgoing")))
                .toList());
        assertThat(outgoing).containsExactly(false, false, true);
    }

    @Test
    void ircIsOneTranscriptLinePerMessage() throws Exception {
        switchTo(MessageLayout.IRC);
        assertThat(onFx(() -> pane.lookup(".chat-pane-list") == null)).as("the list is not shown").isTrue();
        List<String> lines = onFx(ChatPaneUiTest::transcriptLines);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).endsWith(" <alice> Hi!");
        assertThat(lines.get(1)).endsWith(" <alice> Are you there?");
        assertThat(lines.get(2)).endsWith(" <me> Yes.");
    }

    @Test
    void modernHeadsEachGroupOnce() throws Exception {
        switchTo(MessageLayout.MODERN);
        List<String> lines = onFx(ChatPaneUiTest::transcriptLines);
        // Two groups: alice's two messages, then mine.
        assertThat(lines).hasSize(5);
        assertThat(lines.get(0)).startsWith("alice  ");
        assertThat(lines.subList(1, 3)).containsExactly("Hi!", "Are you there?");
        assertThat(lines.get(3)).startsWith("me  ");
        assertThat(lines.get(4)).isEqualTo("Yes.");
    }

    @Test
    void transcriptIsReadOnlyAndKeepsTheSelectionWhenAMessageArrives() throws Exception {
        switchTo(MessageLayout.IRC);
        RichTextArea transcript = onFx(ChatPaneUiTest::transcript);
        assertThat(transcript.isEditable()).isFalse();
        onFx(() -> {
            transcript.select(TextPos.ofLeading(0, 0), TextPos.ofLeading(1, 3));
            pane.getMessages().add(new ChatMessage("bob", "Late to the party.", Instant.parse("2026-09-22T11:00:00Z"), INCOMING));
            return null;
        });
        try {
            assertThat(onFx(ChatPaneUiTest::transcriptLines)).hasSize(4).last().asString().endsWith(" <bob> Late to the party.");
            SelectionSegment selection = onFx(transcript::getSelection);
            assertThat(selection.getMin()).isEqualTo(TextPos.ofLeading(0, 0));
            assertThat(selection.getMax()).isEqualTo(TextPos.ofLeading(1, 3));
        } finally {
            onFx(() -> {
                transcript.clearSelection();
                return pane.getMessages().removeLast();
            });
        }
    }

    @Test
    void timeFormatterAppliesToEveryLayout() throws Exception {
        onFx(() -> {
            pane.setTimeFormatter(DateTimeFormatter.ofPattern("'at' HH:mm").withZone(ZoneOffset.UTC));
            return null;
        });
        try {
            switchTo(MessageLayout.IRC);
            assertThat(onFx(ChatPaneUiTest::transcriptLines).getFirst()).isEqualTo("at 10:00 <alice> Hi!");
            switchTo(MessageLayout.MODERN);
            assertThat(onFx(ChatPaneUiTest::transcriptLines).getFirst()).isEqualTo("alice  at 10:00");
            switchTo(MessageLayout.BUBBLES);
            assertThat(onFx(() -> filledCells().stream()
                    .map(cell -> ((Label) cell.lookup(".message-time")).getText())
                    .toList()))
                    .containsExactly("at 10:00", "at 10:00", "at 10:01");
        } finally {
            onFx(() -> {
                pane.setTimeFormatter(null);
                return null;
            });
        }
    }

    @Test
    void replacedSkinLetsGoOfTheMessages() throws Exception {
        for (MessageLayout layout : MessageLayout.values()) {
            switchTo(layout);
            Node oldView = onFx(() -> pane.getChildrenUnmodifiable().getFirst());
            onFx(() -> {
                pane.setSkin(new ChatPaneSkin(pane));
                return null;
            });
            boolean released = onFx(() -> oldView instanceof RichTextArea area
                    ? area.getModel() == null
                    : ((ListView<?>) oldView).getItems().isEmpty());
            assertThat(released).as("%s view released after dispose", layout).isTrue();
        }
        assertThat(onFx(() -> pane.getMessages().size())).isEqualTo(3);
    }

    private static RichTextArea transcript() {
        return (RichTextArea) pane.lookup(".chat-pane-transcript");
    }

    /// The transcript's paragraphs as plain text. Call on the FX thread.
    private static List<String> transcriptLines() {
        StyledTextModel model = transcript().getModel();
        return IntStream.range(0, model.size()).mapToObj(model::getPlainText).toList();
    }

    /// The non-empty cells in row order. Sorted, because the VirtualFlow keeps recycled cells in
    /// any order among its children. Call on the FX thread.
    private static List<ListCell<?>> filledCells() {
        return pane.lookupAll(".message-cell").stream()
                .<ListCell<?>>map(node -> (ListCell<?>) node)
                .filter(cell -> !cell.isEmpty())
                .sorted(Comparator.comparingInt(ListCell::getIndex))
                .toList();
    }

    /// Tests share one window, so each sets the layout it checks. A layout switch puts a different
    /// view into the skin, which gets its own skin and cells only in the next CSS and layout pass —
    /// run both before the lookup.
    private static void switchTo(MessageLayout layout) throws Exception {
        onFx(() -> {
            pane.setMessageLayout(layout);
            pane.applyCss();
            pane.layout();
            return null;
        });
    }

}
