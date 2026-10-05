package org.jabref.chatpane;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javafx.application.Application;

import org.junit.jupiter.api.extension.ExtendWith;

/// Runs a test class against a JavaFX application, headless (MADR 0008): before the class's first
/// test the application's `start` gets a fresh `Stage` on the FX thread; after its last test the
/// stage's windows are hidden and `stop` is called. See [FxTestExtension].
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(FxTestExtension.class)
public @interface FxTestApplication {

    /// The application to start; it needs a public no-argument constructor.
    Class<? extends Application> value();
}
