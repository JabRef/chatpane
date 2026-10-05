package org.jabref.chatpane;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.jabref.chatpane.FxThread.onFx;

/// The JUnit side of [FxTestApplication], after JabRef's `JavaFxExtension` (MADR 0008).
///
/// Starts the JavaFX toolkit once per test JVM on JavaFX's own headless platform
/// (`glass.platform=Headless`, software rendering): no display, no window on the developer's
/// desktop, no Xvfb in CI. A `-Dglass.platform=…` given to the JVM wins, e.g. to watch a test on
/// the desktop.
///
/// An exception thrown on the FX thread during a test — in a listener, a layout pass — fails that
/// test; outside a test JavaFX would only print it.
public final class FxTestExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final ConcurrentLinkedQueue<Throwable> FX_FAILURES = new ConcurrentLinkedQueue<>();

    private static boolean toolkitStarted;

    private @Nullable Application application;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        startToolkit();
        Class<? extends Application> type = context.getRequiredTestClass().getAnnotation(FxTestApplication.class).value();
        application = type.getConstructor().newInstance();
        onFx(() -> {
            application.start(new Stage());
            return null;
        });
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        FX_FAILURES.clear();
        onFx(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((_, failure) -> FX_FAILURES.add(failure));
            return null;
        });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Lets work the test queued run, so its failures land in this test.
        onFx(() -> null);
        List<Throwable> failures = new ArrayList<>(FX_FAILURES);
        FX_FAILURES.clear();
        if (!failures.isEmpty()) {
            AssertionError error = new AssertionError("Exception on the JavaFX thread", failures.getFirst());
            failures.subList(1, failures.size()).forEach(error::addSuppressed);
            throw error;
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        onFx(() -> {
            List.copyOf(Window.getWindows()).forEach(Window::hide);
            if (application != null) {
                application.stop();
            }
            return null;
        });
        application = null;
    }

    private static synchronized void startToolkit() throws Exception {
        if (toolkitStarted) {
            return;
        }
        if (System.getProperty("glass.platform") == null) {
            System.setProperty("glass.platform", "Headless");
            System.setProperty("prism.order", "sw");
        }
        CompletableFuture<Void> started = new CompletableFuture<>();
        Platform.startup(() -> started.complete(null));
        started.get(30, TimeUnit.SECONDS);
        // Test classes hide their windows one after another; the toolkit stays for the next.
        Platform.setImplicitExit(false);
        toolkitStarted = true;
    }
}
