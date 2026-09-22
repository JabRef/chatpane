plugins {
    `java-library`
    id("chatpane.java-conventions")
}

dependencies {
    api(libs.javafx.controls)
    // IRC and MODERN render as one RichTextArea document (MADR 0010). Incubator
    // module, same license as JavaFX (GPLv2 + Classpath Exception); not part of
    // the API, so `implementation`.
    implementation(libs.jfx.incubator.richtext)
    // MessageRenderer.markdown() (MADR 0011). BSD-2-Clause, real modules, no dependencies.
    implementation(libs.commonmark)
    implementation(libs.commonmark.strikethrough)
    // `requires static` in module-info: the annotations are not needed at run
    // time, but consumers compiling against the API should see them (MADR 0006).
    compileOnlyApi(libs.jspecify)
    // Logging API only (MADR 0005); the application picks the backend.
    implementation(libs.slf4j.api)
}
