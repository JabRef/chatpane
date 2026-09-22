package org.jabref.chatpane;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.skin.ChatPaneSkin;

/// A pane showing a chat conversation.
///
/// Built like the controls in `javafx.scene.control` (MADR 0009): this class holds the state
/// (properties, the message list) and no layout; [ChatPaneSkin] renders it and can be replaced
/// via [#setSkin(Skin)] or `-fx-skin` in CSS.
///
/// Add [ChatMessage]s to [#getMessages()]; the pane shows them in the order of the list, in the
/// [MessageLayout] set through [#messageLayoutProperty()].
/// Switching the layout re-renders the same messages, nothing is lost.
///
/// Styling (MADR 0007): the pane brings no palette of its own; its user-agent stylesheet colors it
/// from the standard Modena lookups (`-fx-accent`, `-fx-base`, `-fx-background`, …), so it looks
/// native in any theme. Hooks: the style class `chat-pane`, one pseudo-class per layout
/// (`:bubbles`, `:irc`, `:modern`), and the cell hooks listed in the README's CSS reference.
/// The layout itself is styleable too: `.chat-pane { -cp-message-layout: bubbles; }`.
// [impl->dsn~chat-pane-control~5]
public class ChatPane extends Control {

    /// The context menu's *Copy*, as [#textLocalizerProperty()] gets it.
    public static final String TEXT_COPY = "Copy";

    /// The context menu's *Select All*, as [#textLocalizerProperty()] gets it.
    public static final String TEXT_SELECT_ALL = "Select All";

    /// The layout a new pane starts with.
    public static final MessageLayout DEFAULT_LAYOUT = MessageLayout.MODERN;

    private static final String STYLE_CLASS = "chat-pane";

    private static final Map<MessageLayout, PseudoClass> LAYOUT_PSEUDO_CLASSES = new EnumMap<>(MessageLayout.class);

    static {
        for (MessageLayout layout : MessageLayout.values()) {
            LAYOUT_PSEUDO_CLASSES.put(layout, PseudoClass.getPseudoClass(layout.cssName()));
        }
    }

    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

    private final ObservableList<MessageAction> messageActions = FXCollections.observableArrayList();

    private final ObjectProperty<UnaryOperator<String>> textLocalizer =
            new SimpleObjectProperty<>(this, "textLocalizer", UnaryOperator.identity());

    private final StyleableObjectProperty<MessageLayout> messageLayout =
            new SimpleStyleableObjectProperty<>(StyleableProperties.MESSAGE_LAYOUT, this, "messageLayout", DEFAULT_LAYOUT) {
                @Override
                protected void invalidated() {
                    updateLayoutPseudoClasses();
                }
            };

    private final ObjectProperty<DateTimeFormatter> timeFormatter =
            new SimpleObjectProperty<>(this, "timeFormatter", defaultTimeFormatter());

    private final ObjectProperty<MessageRenderer> messageRenderer =
            new SimpleObjectProperty<>(this, "messageRenderer", MessageRenderer.plainText());

    private final ObjectProperty<@Nullable Consumer<String>> linkHandler = new SimpleObjectProperty<>(this, "linkHandler");

    public ChatPane() {
        getStyleClass().add(STYLE_CLASS);
        updateLayoutPseudoClasses();
    }

    /// The locale's short time (`15:34`, `3:34 PM`) in the system zone, as of now.
    public static DateTimeFormatter defaultTimeFormatter() {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    }

    /// The messages shown, oldest first.
    public final ObservableList<ChatMessage> getMessages() {
        return messages;
    }

    /// What the user can do with a message: offered in its context menu in every layout, and as
    /// buttons next to a bubble under the pointer ([MessageAction]). Empty by default; *Copy* and
    /// *Select All* are always there.
    public final ObservableList<MessageAction> getMessageActions() {
        return messageActions;
    }

    /// Translates the texts the pane itself shows. It gets the English text — [#TEXT_COPY] and
    /// [#TEXT_SELECT_ALL], the entries of every message's context menu — and returns what to show,
    /// so an application passes its own translation function (JabRef: `Localization::lang`).
    /// Default: the English text; `null` reads as the default. [MessageAction] texts are the
    /// application's own and are shown as given.
    public final ObjectProperty<UnaryOperator<String>> textLocalizerProperty() {
        return textLocalizer;
    }

    public final UnaryOperator<String> getTextLocalizer() {
        return Objects.requireNonNullElse(textLocalizer.get(), UnaryOperator.identity());
    }

    public final void setTextLocalizer(UnaryOperator<String> localizer) {
        textLocalizer.set(localizer);
    }

    /// The layout the messages are shown in. Setting `null` falls back to [#DEFAULT_LAYOUT].
    /// CSS: `-cp-message-layout: bubbles | irc | modern`.
    public final ObjectProperty<MessageLayout> messageLayoutProperty() {
        return messageLayout;
    }

    public final MessageLayout getMessageLayout() {
        // The property is public and bindable, so null can arrive despite the setter's contract.
        return Objects.requireNonNullElse(messageLayout.get(), DEFAULT_LAYOUT);
    }

    public final void setMessageLayout(MessageLayout layout) {
        messageLayout.set(layout);
    }

    /// How a message's time reads, in every layout alike. Setting `null` falls back to
    /// [#defaultTimeFormatter()]; a formatter without a zone formats in the system zone (an
    /// [java.time.Instant] has none of its own).
    public final ObjectProperty<DateTimeFormatter> timeFormatterProperty() {
        return timeFormatter;
    }

    public final DateTimeFormatter getTimeFormatter() {
        DateTimeFormatter formatter = timeFormatter.get();
        if (formatter == null) {
            return defaultTimeFormatter();
        }
        return formatter.getZone() == null ? formatter.withZone(ZoneId.systemDefault()) : formatter;
    }

    public final void setTimeFormatter(DateTimeFormatter formatter) {
        timeFormatter.set(formatter);
    }

    /// How message texts turn into what the layouts show — plain text (the default), Markdown
    /// ([MessageRenderer#markdown()]), or the application's own. Setting `null` falls back to
    /// [MessageRenderer#plainText()]; a change re-renders the shown view.
    public final ObjectProperty<MessageRenderer> messageRendererProperty() {
        return messageRenderer;
    }

    public final MessageRenderer getMessageRenderer() {
        return Objects.requireNonNullElse(messageRenderer.get(), MessageRenderer.plainText());
    }

    public final void setMessageRenderer(MessageRenderer renderer) {
        messageRenderer.set(renderer);
    }

    /// Called with a link's target ([TextSpan#link()]) when the user clicks it, in every layout.
    /// `null` (the default) leaves links styled but inert: the library does not know how the
    /// application opens a URL (in a browser, as a jump to an entry, …).
    public final ObjectProperty<@Nullable Consumer<String>> linkHandlerProperty() {
        return linkHandler;
    }

    public final @Nullable Consumer<String> getLinkHandler() {
        return linkHandler.get();
    }

    public final void setLinkHandler(@Nullable Consumer<String> handler) {
        linkHandler.set(handler);
    }

    private void updateLayoutPseudoClasses() {
        MessageLayout current = getMessageLayout();
        LAYOUT_PSEUDO_CLASSES.forEach((layout, pseudoClass) -> pseudoClassStateChanged(pseudoClass, layout == current));
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ChatPaneSkin(this);
    }

    // Workaround W4 (docs/workarounds.md): the tag sits here because OpenFastTrace does not scan CSS.
    // [impl->dsn~chatpane-stylesheet~5]
    @Override
    public String getUserAgentStylesheet() {
        return Objects.requireNonNull(ChatPane.class.getResource("chatpane.css"), "chatpane.css").toExternalForm();
    }

    /// The CSS properties of this class and its superclasses, as the `javafx.scene.control` convention has it.
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    private static final class StyleableProperties {

        private static final CssMetaData<ChatPane, MessageLayout> MESSAGE_LAYOUT =
                new CssMetaData<>("-cp-message-layout", StyleConverter.getEnumConverter(MessageLayout.class), DEFAULT_LAYOUT) {
                    @Override
                    public boolean isSettable(ChatPane pane) {
                        return !pane.messageLayout.isBound();
                    }

                    @Override
                    public StyleableProperty<MessageLayout> getStyleableProperty(ChatPane pane) {
                        return pane.messageLayout;
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
            styleables.add(MESSAGE_LAYOUT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
}
