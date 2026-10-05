package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.CodeToken;
import org.jabref.chatpane.FxTestApplication;
import org.jabref.chatpane.MessageLayout;
import org.jabref.chatpane.MessageRenderer;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;

/// A code token's style name reaches the shown text in every layout: an application stylesheet
/// colors `token-keyword`, and the keyword is drawn in that color, the code around it not.
// [utest->dsn~code-highlighting~1]
@FxTestApplication(CodeHighlightingUiTest.TestApp.class)
class CodeHighlightingUiTest {

    /// The application's token color.
    private static final String KEYWORD_RED = "data:text/css,.chat-pane .token-keyword{-fx-fill:%23ff0000;}";

    private static ChatPane pane;

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            pane.setMessageRenderer(MessageRenderer.markdown((_, code) -> List.of(
                    new CodeToken(code.substring(0, 4), "keyword"), CodeToken.plain(code.substring(4)))));
            pane.getMessages().add(new ChatMessage("alice", "```\ntrue == x\n```", Instant.parse("2026-09-22T10:00:00Z"), INCOMING));
            Scene scene = new Scene(pane, 480, 300);
            scene.getStylesheets().add(KEYWORD_RED);
            stage.setScene(scene);
            stage.show();
        }
    }

    /// The drawn text `content` — not the bubble's invisible measuring copy (Workaround W5).
    private static Text shown(String content) {
        return pane.lookupAll("Text").stream()
                .filter(node -> node instanceof Text text && content.equals(text.getText())
                        && text.getParent() != null && text.getParent().isVisible())
                .map(node -> (Text) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no shown text '" + content + "'"));
    }

    @Test
    void tokensAreStyledInEveryLayout() throws Exception {
        for (MessageLayout layout : MessageLayout.values()) {
            onFx(() -> {
                pane.setMessageLayout(layout);
                return null;
            });
            settle(pane);
            assertThat(onFx(() -> shown("true").getFill())).as("%s: the keyword", layout).isEqualTo(Color.RED);
            assertThat(onFx(() -> shown(" == x").getFill())).as("%s: the rest", layout).isNotEqualTo(Color.RED);
        }
    }
}
