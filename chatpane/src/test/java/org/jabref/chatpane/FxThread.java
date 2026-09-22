package org.jabref.chatpane;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/// Runs test code on the JavaFX application thread and hands back its result — the one way the UI
/// tests touch nodes.
public final class FxThread {

    private FxThread() {
    }

    /// Lets a few pulses pass, then runs CSS and layout on `root`: a RichTextArea builds its
    /// text cells in a later pulse than the one that laid it out, so nodes inside it (and sizes that
    /// depend on them) exist only afterwards.
    public static void settle(javafx.scene.Parent root) throws Exception {
        for (int pulse = 0; pulse < 3; pulse++) {
            Thread.sleep(50);
            onFx(() -> {
                root.applyCss();
                root.layout();
                return null;
            });
        }
    }

    /// Runs `action` on the FX thread and waits (at most ten seconds) for its result; an exception
    /// thrown there is rethrown here, wrapped.
    public static <T> T onFx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
