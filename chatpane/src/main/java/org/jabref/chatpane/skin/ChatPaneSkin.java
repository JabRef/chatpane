package org.jabref.chatpane.skin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javafx.collections.ListChangeListener;
import javafx.scene.control.SkinBase;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jabref.chatpane.ChatMessage;
import org.jabref.chatpane.ChatPane;
import org.jabref.chatpane.MessageLayout;

/// Default skin of [ChatPane]. It holds one [ConversationView] per [MessageLayout] and shows the
/// one the pane's layout names:
///
/// - [MessageLayout#BUBBLES]: [BubbleView], a virtualized list of talk bubbles (MADR 0009);
/// - [MessageLayout#IRC] and [MessageLayout#MODERN]: [TranscriptView], one read-only document
///   holding the whole conversation (MADR 0010), in the [IrcTranscript] or [ModernTranscript]
///   format.
///
/// The skin itself only switches views and tells the shown one how the messages changed; the views
/// follow the newest message by one shared rule ([ConversationView]).
/// It only displays: composing and sending messages is the application's business.
///
/// Final (Effective Java, item 19): its views are package-private and it has no hooks worth
/// overriding; an application that wants another look writes its own skin and sets it with
/// [ChatPane#setSkin] or `-fx-skin`.
// [impl->dsn~chat-pane-skin~7]
// [impl->dsn~no-input-in-the-pane~1]
public final class ChatPaneSkin extends SkinBase<ChatPane> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPaneSkin.class);

    /// The one place that knows which view shows which layout.
    private final Map<MessageLayout, ConversationView> views = new EnumMap<>(MessageLayout.class);

    private @Nullable ConversationView shown;

    private final StyleProbe probe = new StyleProbe(this::restyle);

    public ChatPaneSkin(ChatPane control) {
        super(control);
        RenderContext context = RenderContext.of(control);
        views.put(MessageLayout.BUBBLES, new BubbleView(context));
        views.put(MessageLayout.IRC, new TranscriptView(new IrcTranscript(context), context));
        views.put(MessageLayout.MODERN, new TranscriptView(new ModernTranscript(context), context));

        registerChangeListener(control.messageLayoutProperty(), _ -> showLayout());
        // How a message reads changed: render the shown view again.
        registerChangeListener(control.timeFormatterProperty(), _ -> rerender());
        registerChangeListener(control.messageRendererProperty(), _ -> rerender());
        // The bubbles' action buttons are built with the cells.
        registerListChangeListener(control.getMessageActions(), _ -> rerender());
        registerListChangeListener(control.getMessages(), this::messagesChanged);
        showLayout();
    }

    /// Listeners registered through `SkinBase` go away by themselves; the shown view has to let go
    /// of the messages, so a replaced skin keeps nothing alive.
    @Override
    public void dispose() {
        if (shown != null) {
            shown.hide();
            shown = null;
        }
        super.dispose();
    }

    private void showLayout() {
        MessageLayout layout = getSkinnable().getMessageLayout();
        ConversationView next = views.get(layout);
        if (shown != null && shown != next) {
            shown.hide();
        }
        LOGGER.debug("Showing layout {}", layout);
        // On screen first: the view builds text cells as it is shown, with the styles it can
        // resolve at that moment.
        getChildren().setAll(next.node(), probe);
        next.show(getSkinnable().getMessages());
        shown = next;
    }

    private void restyle() {
        if (shown != null) {
            LOGGER.debug("Styles changed, redrawing the shown view");
            shown.restyle();
        }
    }

    private void rerender() {
        if (shown != null) {
            shown.replaced(getSkinnable().getMessages());
        }
    }

    private void messagesChanged(ListChangeListener.Change<?> change) {
        if (shown == null) {
            return;
        }
        List<ChatMessage> messages = getSkinnable().getMessages();
        int from = MessageChanges.appendedFrom(change);
        int replaced = MessageChanges.replacedAt(change);
        if (from != MessageChanges.NOT_AN_APPEND) {
            shown.appended(messages, from);
        } else if (replaced != MessageChanges.NOT_AN_APPEND) {
            shown.updated(messages, replaced);
        } else {
            shown.replaced(messages);
        }
    }
}
