package org.jabref.chatpane.skin;

import javafx.scene.control.Label;

/// An invisible node in the skin that notices when the styles the pane's text is drawn with change
/// — another theme, a stylesheet added or removed, another font size — and calls back.
///
/// Needed because a [jfx.incubator.scene.control.richtext.RichTextArea] resolves a segment's style
/// names into styles once, when it builds the text cell, and keeps the cell: its text never follows
/// a CSS change by itself, and cells it built before the pane's CSS was in place keep the defaults
/// (Workaround W7). `chatpane.css` gives the probe a background from every lookup the text uses and
/// the text font; when CSS changes any of them, the probe's background or font changes.
// Workaround W7 (docs/workarounds.md).
// [impl->dsn~transcript-restyle~1]
final class StyleProbe extends Label {

    StyleProbe(Runnable onChange) {
        getStyleClass().add("style-probe");
        setManaged(false);
        setVisible(false);
        setMouseTransparent(true);
        backgroundProperty().addListener((_, _, _) -> onChange.run());
        fontProperty().addListener((_, _, _) -> onChange.run());
    }
}
