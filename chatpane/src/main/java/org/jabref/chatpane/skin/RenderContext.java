package org.jabref.chatpane.skin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.MessageAction;
import org.jabref.chatpane.MessageRenderer;
import org.jabref.chatpane.TextLine;

/// What every view needs from the pane to render a message, read fresh each time: the time
/// format and zone, the message renderer, the link handler and the message actions. One object for both views, so they cannot
/// drift apart in how a message reads (review finding S5).
final class RenderContext {

    private final Function<Instant, String> time;
    private final Supplier<MessageRenderer> renderer;
    private final Supplier<@Nullable Consumer<String>> linkHandler;
    private final Supplier<List<MessageAction>> actions;
    private final Supplier<ZoneId> zone;
    private final Supplier<UnaryOperator<String>> localizer;

    RenderContext(Function<Instant, String> time, Supplier<MessageRenderer> renderer,
            Supplier<@Nullable Consumer<String>> linkHandler) {
        this(time, renderer, linkHandler, List::of, ZoneId::systemDefault, UnaryOperator::identity);
    }

    RenderContext(Function<Instant, String> time, Supplier<MessageRenderer> renderer,
            Supplier<@Nullable Consumer<String>> linkHandler, Supplier<List<MessageAction>> actions, Supplier<ZoneId> zone,
            Supplier<UnaryOperator<String>> localizer) {
        this.time = time;
        this.renderer = renderer;
        this.linkHandler = linkHandler;
        this.actions = actions;
        this.zone = zone;
        this.localizer = localizer;
    }

    /// The context of a live pane: its current properties, whenever asked.
    static RenderContext of(ChatPane pane) {
        return new RenderContext(instant -> pane.getTimeFormatter().format(instant), pane::getMessageRenderer,
                pane::getLinkHandler, pane::getMessageActions, () -> pane.getTimeFormatter().getZone(),
                pane::getTextLocalizer);
    }

    String time(Instant instant) {
        return time.apply(instant);
    }

    /// Date and time in full, for a tooltip: the locale's medium format in the pane's time zone.
    String fullTime(Instant instant) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zone.get()).format(instant);
    }

    /// One of the pane's own texts (`ChatPane.TEXT_…`) in the application's language.
    String localize(String english) {
        return localizer.get().apply(english);
    }

    /// The pane's actions that apply to `message`, in order.
    List<MessageAction> actionsFor(ChatMessage message) {
        return actions.get().stream().filter(action -> action.appliesTo().test(message)).toList();
    }

    List<TextLine> lines(ChatMessage message) {
        return renderer.get().render(message.text());
    }

    int lineCount(ChatMessage message) {
        return renderer.get().lineCount(message.text());
    }

    /// Hands `target` to the pane's link handler, if there is one.
    void openLink(String target) {
        @Nullable Consumer<String> handler = linkHandler.get();
        if (handler != null) {
            handler.accept(target);
        }
    }

    /// Whether clicking a link does anything — the hand cursor promises it does.
    boolean linksActive() {
        return linkHandler.get() != null;
    }
}
