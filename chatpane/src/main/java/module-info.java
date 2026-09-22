/// A JavaFX control that shows a chat conversation in a selectable layout.
///
/// Start with [org.jabref.chatpane.ChatPane].
// [impl->dsn~module-descriptor~3]
module org.jabref.chatpane {
    requires transitive javafx.controls;
    // Not transitive: no incubator type appears in the API (MADR 0010).
    requires jfx.incubator.richtext;
    // MessageRenderer.markdown() (MADR 0011); not transitive, no commonmark type in the API.
    requires org.commonmark;
    requires org.commonmark.ext.gfm.strikethrough;
    // Transitive: @Nullable is part of the public API (JSpecify's recommended form).
    requires static transitive org.jspecify;
    requires org.slf4j;

    exports org.jabref.chatpane;
    exports org.jabref.chatpane.skin;
}
