package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Paint;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import io.gitlab.fxlabs.testfx.junit.jupiter.TestFxApplication;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.MessageLayout;
import org.jabref.chatpane.MessageRenderer;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;

/// The transcript is styled right when it is first shown and after a theme change. The area
/// resolves a segment's style names once, when it builds the text cell, and keeps the cell: cells
/// built before the pane's CSS was in place stayed unstyled (Carl, 2026-09-22: "When selecting
/// modern or irc it shows initially without styling"), and a theme switch re-applied
/// `-fx-wrap-text`, which made the area lay out in the middle of the CSS pass and fail
/// ("duplicate children added").
// [utest->dsn~transcript-restyle~1]
@Tag("ui")
@TestFxApplication(TranscriptStylingUiTest.TestApp.class)
class TranscriptStylingUiTest {

    /// A dark theme the way an application ships one: Modena's variables, as an author stylesheet.
    private static final String DARK = "data:text/css,.root{-fx-base:%233c3f41;-fx-background:%232b2b2b;"
            + "-fx-control-inner-background:%23232425;}";

    private static ChatPane pane;

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            pane.setMessageRenderer(MessageRenderer.markdown());
            pane.setMessageLayout(MessageLayout.BUBBLES);
            pane.getMessages().addAll(List.of(
                    new ChatMessage("alice", "Morning! Did the build go green?", Instant.parse("2026-09-22T10:00:00Z"), INCOMING),
                    new ChatMessage("bob", "It did.", Instant.parse("2026-09-22T10:10:00Z"), INCOMING)));
            stage.setScene(new Scene(pane, 480, 300));
            stage.show();
        }
    }

    private static Text shown(String content) {
        return pane.lookup(".chat-pane-transcript").lookupAll("Text").stream()
                .filter(node -> node instanceof Text text && content.equals(text.getText()))
                .map(node -> (Text) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no shown text '" + content + "'"));
    }

    @Test
    void transcriptIsStyledWhenFirstShownAndAfterAThemeChange() throws Exception {
        List<Throwable> failures = new ArrayList<>();
        onFx(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((_, e) -> failures.add(e));
            return null;
        });
        try {
            for (MessageLayout layout : List.of(MessageLayout.IRC, MessageLayout.MODERN)) {
                onFx(() -> {
                    pane.setMessageLayout(MessageLayout.BUBBLES);
                    return null;
                });
                settle(pane);
                onFx(() -> {
                    pane.setMessageLayout(layout);
                    return null;
                });
                settle(pane);
                String sender = layout == MessageLayout.IRC ? "<alice>" : "alice";
                Paint senderFill = onFx(() -> shown(sender).getFill());
                Paint textFill = onFx(() -> shown("Morning! Did the build go green?").getFill());
                assertThat(senderFill).as("%s: sender in the accent, text in the text color", layout).isNotEqualTo(textFill);
                assertThat(onFx(() -> shown("Morning! Did the build go green?").getFont().getStyle()))
                        .as("%s: plain text is not italic", layout).doesNotContainIgnoringCase("italic");

                onFx(() -> {
                    pane.getScene().getStylesheets().add(DARK);
                    return null;
                });
                settle(pane);
                Paint darkTextFill = onFx(() -> shown("Morning! Did the build go green?").getFill());
                onFx(() -> {
                    pane.getScene().getStylesheets().remove(DARK);
                    return null;
                });
                settle(pane);
                assertThat(darkTextFill).as("%s: text restyled for the dark theme", layout).isNotEqualTo(textFill);
                assertThat(onFx(() -> shown("Morning! Did the build go green?").getFill()))
                        .as("%s: and back", layout).isEqualTo(textFill);
            }
            assertThat(failures).as("exceptions on the FX thread").isEmpty();
        } finally {
            onFx(() -> {
                Thread.currentThread().setUncaughtExceptionHandler(null);
                return null;
            });
        }
    }
}
