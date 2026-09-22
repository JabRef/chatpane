/// Demo application for the chat pane.
module org.jabref.chatpane.demo {
    requires org.jabref.chatpane;
    requires static org.jspecify;
    requires org.slf4j;

    // The JavaFX launcher instantiates DemoApp reflectively.
    exports org.jabref.chatpane.demo to javafx.graphics;
}
