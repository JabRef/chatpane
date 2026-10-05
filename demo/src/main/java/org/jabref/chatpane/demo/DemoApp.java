package org.jabref.chatpane.demo;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.MessageAction;
import org.jabref.chatpane.MessageLayout;
import org.jabref.chatpane.MessageRenderer;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.ChatMessage.Direction.OUTGOING;

/// Shows a sample conversation in a [ChatPane]: toggles for the layout and the text format (plain,
/// Markdown) top left, a light/dark/system theme toggle top right ([DemoTheme]), an input line at
/// the bottom that appends the typed text as the local user's message — answered by a pretend
/// assistant whose reply grows in place ([DemoResponder]) — *Delete* and *Retry* as message
/// actions (*Retry* greyed out while an answer is being written), and a find bar on <kbd>Ctrl</kbd> + <kbd>F</kbd>.
// [impl->dsn~demo-app~4]
public class DemoApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoApp.class);

    private static final String LOCAL_USER = "me";

    /// How the demo renders message texts ([ChatPane#messageRendererProperty()]).
    enum TextFormat {
        PLAIN(MessageRenderer.plainText()), MARKDOWN(MessageRenderer.markdown(new JsonHighlighter()));

        private final MessageRenderer renderer;

        TextFormat(MessageRenderer renderer) {
            this.renderer = renderer;
        }
    }

    @Override
    public void start(Stage stage) {
        ChatPane chat = new ChatPane();
        chat.getMessages().addAll(sampleConversation(Instant.now()));
        // `--layout=bubbles`, `--theme=dark`, `--text=plain` pick the start values, e.g. for screenshots.
        chat.setMessageLayout(namedArgument("layout", MessageLayout.class, chat.getMessageLayout()));
        DemoTheme theme = namedArgument("theme", DemoTheme.class, DemoTheme.SYSTEM);
        TextFormat format = namedArgument("text", TextFormat.class, TextFormat.MARKDOWN);
        chat.setMessageRenderer(format.renderer);
        chat.setLinkHandler(url -> getHostServices().showDocument(url));
        DemoResponder responder = new DemoResponder(chat.getMessages());
        MessageAction retry = new MessageAction("Retry",
                message -> message.status() == ChatMessage.Status.ERROR && message.sender().equals(DemoResponder.NAME),
                responder::retry);
        // One answer at a time: the buttons and menu items grey out while one is being written.
        retry.disableProperty().bind(Bindings.createBooleanBinding(
                () -> chat.getMessages().stream().anyMatch(m -> m.status() == ChatMessage.Status.PENDING),
                chat.getMessages()));
        chat.getMessageActions().addAll(
                new MessageAction("Delete", message -> chat.getMessages().removeIf(m -> m == message)),
                retry);

        BorderPane root = new BorderPane(chat);
        Scene scene = new Scene(root, 760, 640);
        // The demo's own token colors for highlighted code; dark.css overrides them.
        scene.getStylesheets().add(Objects.requireNonNull(DemoApp.class.getResource("demo.css"), "demo.css").toExternalForm());
        DemoTheme.install(scene);
        theme.applyTo(scene);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8,
                toggles("Layout:", "layout", MessageLayout.values(), DemoApp::label,
                        chat.getMessageLayout(), chat::setMessageLayout),
                toggles("Text:", "text", TextFormat.values(), DemoApp::label, format,
                        chosen -> chat.setMessageRenderer(chosen.renderer)),
                spacer,
                toggles("Theme:", "theme", DemoTheme.values(), DemoTheme::label, theme, chosen -> chosen.applyTo(scene)));
        top.setPadding(new Insets(8));
        top.setAlignment(Pos.CENTER_LEFT);
        HBox findBar = findBar(chat);
        root.setTop(new VBox(top, findBar));
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+F"), () -> openFind(findBar));
        root.setBottom(inputLine(chat, responder));

        stage.setTitle("ChatPane demo");
        stage.setScene(scene);
        stage.show();
        LOGGER.info("Demo started with {} sample messages", chat.getMessages().size());
    }

    /// The toggle label of a layout — UI text, not the CSS name the library gives it.
    private static String label(MessageLayout layout) {
        return switch (layout) {
            case BUBBLES -> "Bubbles";
            case IRC -> "IRC";
            case MODERN -> "Modern";
        };
    }

    private static String label(TextFormat format) {
        return switch (format) {
            case PLAIN -> "Plain";
            case MARKDOWN -> "Markdown";
        };
    }

    private <E extends Enum<E>> E namedArgument(String name, Class<E> type, E fallback) {
        String value = getParameters().getNamed().get(name);
        return value == null ? fallback : Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    /// A label and one toggle per value, exactly one selected at all times; picking one calls `onSelect`.
    private static <E extends Enum<E>> HBox toggles(String caption, String idPrefix, E[] values,
            Function<E, String> text, E selected, Consumer<E> onSelect) {
        ToggleGroup group = new ToggleGroup();
        HBox bar = new HBox(8, new Label(caption));
        for (E value : values) {
            ToggleButton button = new ToggleButton(text.apply(value));
            button.setId(idPrefix + "-" + value.name().toLowerCase(Locale.ROOT));
            button.setToggleGroup(group);
            button.setSelected(value == selected);
            button.setOnAction(_ -> {
                // A second click would deselect the toggle; there is always a choice.
                button.setSelected(true);
                onSelect.accept(value);
            });
            bar.getChildren().add(button);
        }
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    /// The find bar, hidden until <kbd>Ctrl</kbd> + <kbd>F</kbd>: the query field drives the pane's
    /// find query, <kbd>Enter</kbd> and <kbd>Shift</kbd> + <kbd>Enter</kbd> step through the
    /// matches, <kbd>Esc</kbd> closes the bar and clears the query.
    // [impl->dsn~demo-find-bar~1]
    private static HBox findBar(ChatPane chat) {
        TextField query = new TextField();
        query.setId("find-input");
        query.setPromptText("Find");
        query.textProperty().bindBidirectional(chat.findQueryProperty());
        Label count = new Label();
        count.setId("find-count");
        count.textProperty().bind(Bindings.createStringBinding(
                () -> chat.getFindQuery().isEmpty() ? "" : (chat.getFindIndex() + 1) + "/" + chat.getFindMatches().size(),
                chat.findQueryProperty(), chat.findIndexProperty(), chat.getFindMatches()));
        Button previous = new Button("Previous");
        previous.setOnAction(_ -> chat.findPrevious());
        Button next = new Button("Next");
        next.setOnAction(_ -> chat.findNext());
        Button close = new Button("Close");
        HBox.setHgrow(query, Priority.ALWAYS);
        HBox bar = new HBox(8, query, count, previous, next, close);
        bar.setId("find-bar");
        bar.setPadding(new Insets(0, 8, 8, 8));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.managedProperty().bind(bar.visibleProperty());
        bar.setVisible(false);
        close.setOnAction(_ -> closeFind(bar, chat));
        query.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    chat.findPrevious();
                } else {
                    chat.findNext();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                closeFind(bar, chat);
                event.consume();
            }
        });
        return bar;
    }

    private static void openFind(HBox findBar) {
        findBar.setVisible(true);
        TextField query = (TextField) findBar.getChildren().getFirst();
        query.requestFocus();
        query.selectAll();
    }

    private static void closeFind(HBox findBar, ChatPane chat) {
        findBar.setVisible(false);
        chat.setFindQuery("");
    }

    private static HBox inputLine(ChatPane chat, DemoResponder responder) {
        TextField input = new TextField();
        input.setId("message-input");
        input.setPromptText("Message");
        Button send = new Button("Send");
        send.setDefaultButton(true);
        send.disableProperty().bind(input.textProperty().isEmpty());
        send.setOnAction(_ -> {
            String text = input.getText().strip();
            if (!text.isEmpty()) {
                chat.getMessages().add(new ChatMessage(LOCAL_USER, text, Instant.now(), OUTGOING));
                responder.answer(text);
            }
            input.clear();
        });
        HBox.setHgrow(input, Priority.ALWAYS);
        HBox line = new HBox(8, input, send);
        line.setPadding(new Insets(8));
        return line;
    }

    private static List<ChatMessage> sampleConversation(Instant now) {
        Instant start = now.minusSeconds(3600);
        return List.of(
                new ChatMessage("alice", "Morning! Did the build go green?", start, INCOMING),
                new ChatMessage("alice", "The one with the new layout switch.", start.plusSeconds(20), INCOMING),
                new ChatMessage(LOCAL_USER, "It did. Try the buttons on top: bubbles, irc, modern.", start.plusSeconds(90), OUTGOING),
                new ChatMessage("bob", "Bubbles for me. Long messages should wrap instead of running off the edge of the pane,"
                        + " so here is one that is long enough to show whether they do.", start.plusSeconds(400), INCOMING),
                new ChatMessage("alice", "IRC, *obviously*. And **Markdown** now works in every layout:\n"
                        + "- `code`, ~~strike~~ and [links](https://github.com/calixtus/chatpane)\n"
                        + "- lists\n  1. nested\n  2. numbered\n\n> a quote", start.plusSeconds(460), INCOMING),
                new ChatMessage("bob", """
                        And code blocks are highlighted, here as JSON:
                        ```json
                        {
                          "layout": "bubbles",
                          "width": 0.7,
                          "markdown": true,
                          "theme": null
                        }
                        ```""", start.plusSeconds(520), INCOMING),
                new ChatMessage(LOCAL_USER, "Noted.", start.plusSeconds(2400), OUTGOING),
                new ChatMessage(LOCAL_USER, "Consecutive messages within five minutes share one header.", start.plusSeconds(2410), OUTGOING));
    }

    public static void main(String[] args) {
        launch(DemoApp.class, args);
    }
}
