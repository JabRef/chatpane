package org.jabref.chatpane.skin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
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
import static org.jabref.chatpane.ChatMessage.Direction.OUTGOING;
import static org.jabref.chatpane.FxThread.onFx;
import static org.jabref.chatpane.FxThread.settle;
import static org.assertj.core.api.Assertions.assertThat;

/// The body of a bubble, a read-only RichTextArea: as tall as its text, as wide as its widest line
/// up to the bubble's share, its selection visible on either bubble kind, Markdown styled, links
/// clickable. Order of the three messages: short incoming, long incoming, outgoing.
// [utest->dsn~bubble-text~1]
// [utest->dsn~message-links~1]
@Tag("ui")
@TestFxApplication(BubbleTextUiTest.TestApp.class)
class BubbleTextUiTest {

    private static final Instant T0 = Instant.parse("2026-09-22T10:00:00Z");

    private static ChatPane pane;

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            pane = new ChatPane();
            pane.setMessageLayout(MessageLayout.BUBBLES);
            pane.setMessageRenderer(MessageRenderer.markdown());
            pane.getMessages().addAll(List.of(
                    new ChatMessage("alice", "Short **bold** [link](https://example.org).", T0, INCOMING),
                    new ChatMessage("bob", "Long messages should wrap instead of running off the edge of the pane,"
                            + " so here is one that is long enough to wrap over several lines.", T0.plusSeconds(600), INCOMING),
                    new ChatMessage("me", "Mine goes on the right, in the accent color.", T0.plusSeconds(1200), OUTGOING)));
            stage.setScene(new Scene(pane, 480, 360));
            stage.show();
        }
    }

    /// The laid-out bodies in message order. Call on the FX thread.
    private static List<BubbleText> bodies() {
        pane.applyCss();
        pane.layout();
        List<BubbleText> bodies = new ArrayList<>(pane.lookupAll(".message-body").stream()
                .map(node -> (BubbleText) node)
                .filter(body -> body.getScene() != null && body.getWidth() > 0)
                .toList());
        bodies.sort(Comparator.comparingDouble(body -> body.localToScene(0, 0).getY()));
        return bodies;
    }

    @Test
    void bodyIsAsTallAsItsTextAndShrinksToShortText() throws Exception {
        settle(pane);
        List<double[]> sizes = onFx(() -> bodies().stream()
                .map(body -> new double[] {body.getWidth(), body.getHeight(), body.prefHeight(body.getWidth())})
                .toList());
        assertThat(sizes).hasSize(3);
        for (double[] size : sizes) {
            assertThat(size[1]).as("height vs content height").isGreaterThanOrEqualTo(size[2] - 0.5);
        }
        double listShare = 480 * 0.7;
        assertThat(sizes.get(0)[0]).as("short bubble shrinks").isLessThan(listShare / 2);
        assertThat(sizes.get(1)[1]).as("long message wraps onto several lines").isGreaterThan(2 * sizes.get(0)[1]);
    }

    /// Modena's selection highlight is a light accent, close to an outgoing bubble's own color
    /// (Carl, 2026-09-22, about the first TextArea bodies). Checked by brightness difference.
    @Test
    void selectionContrastsWithItsBubble() throws Exception {
        settle(pane);
        List<double[]> brightness = onFx(() -> {
            List<double[]> pairs = new ArrayList<>();
            for (BubbleText body : bodies()) {
                body.selectAll();
                pane.applyCss();
                pane.layout();
                Path highlight = (Path) body.lookup(".selection-highlight");
                pairs.add(new double[] {luminance(highlight.getFill()) * highlight.getOpacity()
                        + luminance(bubbleFill(body)) * (1 - highlight.getOpacity()), luminance(bubbleFill(body))});
                body.clearSelection();
            }
            return pairs;
        });
        assertThat(brightness).hasSize(3);
        for (double[] pair : brightness) {
            assertThat(Math.abs(pair[0] - pair[1])).as("selected vs bubble brightness").isGreaterThan(0.1);
        }
    }

    @Test
    void bodyIsReadOnlyAndStyledByMarkdown() throws Exception {
        settle(pane);
        BubbleText first = onFx(() -> bodies().getFirst());
        assertThat(first.isEditable()).isFalse();
        assertThat(first.getStyleClass()).contains("rich-text-area", "message-body");
        Text bold = onFx(() -> shownText(first, "bold"));
        assertThat(bold.getText()).isEqualTo("bold");
        assertThat(bold.getFont().getStyle()).containsIgnoringCase("bold");
    }

    @Test
    void clickingALinkHandsItsTargetToTheHandler() throws Exception {
        List<String> opened = new ArrayList<>();
        onFx(() -> {
            pane.setLinkHandler(opened::add);
            return null;
        });
        settle(pane);
        try {
            onFx(() -> {
                BubbleText first = bodies().getFirst();
                Text link = shownText(first, "link");
                Bounds bounds = link.localToScreen(link.getBoundsInLocal());
                Point2D local = first.screenToLocal(bounds.getCenterX(), bounds.getCenterY());
                first.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, local.getX(), local.getY(),
                        bounds.getCenterX(), bounds.getCenterY(), MouseButton.PRIMARY, 1,
                        false, false, false, false, true, false, false, true, false, true, null));
                return null;
            });
            assertThat(opened).containsExactly("https://example.org");
        } finally {
            onFx(() -> {
                pane.setLinkHandler(null);
                return null;
            });
        }
    }

    /// The shown `Text` holding exactly `content`. By text, not by style class: the area resolves
    /// a segment's style names into styles and draws plain `Text` nodes without the names.
    private static Text shownText(BubbleText body, String content) {
        return body.lookupAll("Text").stream()
                .filter(node -> node instanceof Text text && content.equals(text.getText()))
                .map(node -> (Text) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no shown text '" + content + "'"));
    }

    private static Paint bubbleFill(Node node) {
        Node bubble = node;
        while (!bubble.getStyleClass().contains("message-bubble")) {
            bubble = bubble.getParent();
        }
        return ((Region) bubble).getBackground().getFills().getFirst().getFill();
    }

    private static double luminance(Paint paint) {
        Color color = (Color) paint;
        return 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
    }
}
