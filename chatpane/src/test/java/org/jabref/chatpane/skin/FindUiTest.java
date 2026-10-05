package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.skin.VirtualFlow;
import javafx.stage.Stage;

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;

import io.gitlab.fxlabs.testfx.junit.jupiter.TestFxApplication;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.FindMatch;
import org.jabref.chatpane.MessageLayout;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;

/// Find: the pane's state, the highlights every layout draws, and scrolling to the current match.
@Tag("ui")
@TestFxApplication(FindUiTest.TestApp.class)
class FindUiTest {

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

    /// Messages ten minutes apart, so none continues a group: `texts` in `layout`, laid out.
    private static void show(MessageLayout layout, List<String> texts) throws Exception {
        onFx(() -> {
            pane.setFindQuery("");
            pane.setMessageLayout(layout);
            pane.getMessages().setAll(IntStream.range(0, texts.size())
                    .mapToObj(i -> new ChatMessage("alice", texts.get(i), T0.plusSeconds(600L * i), INCOMING))
                    .toList());
            pane.applyCss();
            pane.layout();
            return null;
        });
        settle(pane);
    }

    /// Forty messages, far more than fit, the first one holding `needle`.
    private static List<String> longConversation(String needle) {
        return IntStream.range(0, 40).mapToObj(i -> i == 0 ? "the " + needle + " here" : "Message " + i).toList();
    }

    private static void find(String query) throws Exception {
        onFx(() -> {
            pane.setFindQuery(query);
            return null;
        });
        settle(pane);
    }

    private static long highlights(String styleClass) throws Exception {
        return onFx(() -> pane.lookupAll("." + styleClass).size());
    }

    // [utest->dsn~find-in-messages~1]
    @Test
    void stateFollowsQueryMessagesAndSteps() throws Exception {
        show(MessageLayout.MODERN, List.of("cocoa", "none", "Coconut"));
        onFx(() -> {
            pane.setFindQuery("co");
            assertThat(pane.getFindMatches()).containsExactly(
                    new FindMatch(0, 0, 0, 2), new FindMatch(0, 0, 2, 4), new FindMatch(2, 0, 0, 2), new FindMatch(2, 0, 2, 4));
            assertThat(pane.getFindIndex()).isZero();

            pane.findPrevious();
            assertThat(pane.getFindIndex()).as("before the first: the last").isEqualTo(3);
            pane.findNext();
            assertThat(pane.getFindIndex()).as("after the last: the first").isZero();
            pane.findNext();

            pane.getMessages().add(new ChatMessage("bob", "cold", T0.plusSeconds(6000), INCOMING));
            assertThat(pane.getFindMatches()).hasSize(5);
            assertThat(pane.getFindIndex()).as("a new message moves nothing").isEqualTo(1);

            pane.getMessages().remove(1, pane.getMessages().size());
            assertThat(pane.getFindMatches()).hasSize(2);
            assertThat(pane.getFindIndex()).as("unchanged, still in range").isEqualTo(1);
            pane.getMessages().setAll(new ChatMessage("bob", "co", T0, INCOMING));
            assertThat(pane.getFindIndex()).as("cut to the last match").isZero();

            pane.setFindQuery("absent");
            assertThat(pane.getFindMatches()).isEmpty();
            assertThat(pane.getFindIndex()).isEqualTo(-1);
            assertThat(pane.getCurrentFindMatch()).isNull();

            pane.setFindQuery(null);
            assertThat(pane.getFindQuery()).isEmpty();
            return null;
        });
    }

    // [utest->dsn~find-highlights~1]
    @Test
    void everyLayoutHighlightsTheMatchesAndTheCurrentOne() throws Exception {
        for (MessageLayout layout : MessageLayout.values()) {
            show(layout, List.of("cocoa", "none", "Coconut"));
            find("co");
            assertThat(highlights("find-match")).as("%s: all matches", layout).isEqualTo(4);
            assertThat(highlights("find-current")).as("%s: one current", layout).isEqualTo(1);

            find("");
            assertThat(highlights("find-match")).as("%s: cleared", layout).isZero();
        }
    }

    // [utest->dsn~find-highlights~1]
    @Test
    void highlightsFollowTheCurrentMatch() throws Exception {
        show(MessageLayout.IRC, List.of("co", "co"));
        find("co");
        onFx(() -> {
            pane.findNext();
            return null;
        });
        settle(pane);
        assertThat(highlights("find-match")).isEqualTo(2);
        assertThat(highlights("find-current")).isEqualTo(1);
    }

    // [utest->dsn~find-reveal~1]
    @Test
    void transcriptSelectsTheCurrentMatch() throws Exception {
        show(MessageLayout.IRC, longConversation("needle"));
        find("needle");
        String selected = onFx(() -> {
            RichTextArea area = (RichTextArea) pane.lookup(".chat-pane-transcript");
            SelectionSegment selection = area.getSelection();
            String paragraph = area.getModel().getPlainText(selection.getMin().index());
            return paragraph.substring(selection.getMin().offset(), selection.getMax().offset());
        });
        assertThat(selected).isEqualTo("needle");
    }

    // [utest->dsn~find-reveal~1]
    @Test
    void bubblesScrollTheMatchIntoView() throws Exception {
        show(MessageLayout.BUBBLES, longConversation("needle"));
        assertThat(onFx(() -> ((VirtualFlow<?>) pane.lookup(".virtual-flow")).getFirstVisibleCell().getIndex()))
                .as("followed the newest").isPositive();
        find("needle");
        assertThat(onFx(() -> ((VirtualFlow<?>) pane.lookup(".virtual-flow")).getFirstVisibleCell().getIndex()))
                .isZero();
    }
}
