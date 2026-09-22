package org.jabref.chatpane.demo;

import java.util.Locale;
import java.util.Objects;

import javafx.application.ColorScheme;
import javafx.scene.Scene;

import org.jspecify.annotations.Nullable;

/// The demo's theme choice: light, dark, or whatever the operating system asks for.
///
/// The choice goes into the scene's own color-scheme preference (`null` = follow the platform),
/// which also switches the window decorations; the scene's effective scheme then decides whether
/// `dark.css`, the demo's dark Modena, is on. The dark theme is the demo's own, as an
/// application's theme would be: the library brings none and must look right in both
/// (MADR 0007) — which is what this switch lets one check.
// [impl->dsn~demo-theme-switch~1]
enum DemoTheme {

    LIGHT(ColorScheme.LIGHT), DARK(ColorScheme.DARK), SYSTEM(null);

    private static final String DARK_STYLESHEET =
            Objects.requireNonNull(DemoTheme.class.getResource("dark.css"), "dark.css").toExternalForm();

    private final @Nullable ColorScheme scheme;

    DemoTheme(@Nullable ColorScheme scheme) {
        this.scheme = scheme;
    }

    /// The toggle label, e.g. `Light`.
    String label() {
        String name = name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /// Makes `scene` follow the theme set with [#applyTo(Scene)] from now on.
    static void install(Scene scene) {
        scene.getPreferences().colorSchemeProperty().subscribe(effective -> {
            if (effective == ColorScheme.DARK) {
                if (!scene.getStylesheets().contains(DARK_STYLESHEET)) {
                    scene.getStylesheets().add(DARK_STYLESHEET);
                }
            } else {
                scene.getStylesheets().remove(DARK_STYLESHEET);
            }
        });
    }

    void applyTo(Scene scene) {
        scene.getPreferences().setColorScheme(scheme);
    }
}
