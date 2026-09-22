package org.jabref.chatpane.skin;

import java.util.List;

import org.jabref.chatpane.ChatMessage;

/// How one message turns into built paragraphs: [IrcTranscript] and [ModernTranscript] for the
/// transcript, [BodyFormat] for the body of a bubble. Building blocks shared by all three are in
/// [TranscriptSegments]; the message text itself comes from the pane's
/// [org.jabref.chatpane.MessageRenderer] via the [RenderContext].
///
/// [TranscriptModel] asks for [#paragraphCount] of every message but for [#paragraphs] only of
/// the ones on screen, so the count must not build anything — and must match what
/// [#paragraphs] would build.
// [impl->dsn~transcript-paragraphs~4]
interface TranscriptFormat {

    /// The paragraphs of one message; `continued` if it continues the group of the one before
    /// ([MessageGrouping]).
    List<TranscriptLine> paragraphs(ChatMessage message, boolean continued);

    /// How many paragraphs [#paragraphs] returns for these arguments, without building them.
    int paragraphCount(ChatMessage message, boolean continued);
}
