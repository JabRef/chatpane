import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    `java-library`
    id("chatpane.java-conventions")
    alias(libs.plugins.maven.publish)
}

group = "org.jabref"
// -PversionSuffix=PR17 turns 0.1.0-SNAPSHOT into 0.1.0-PR17-SNAPSHOT, so a pull request's
// snapshot is identifiable and does not clobber the one built from main (publish.yml).
val baseVersion = providers.gradleProperty("chatpaneVersion").get()
version = providers.gradleProperty("versionSuffix")
    .map { baseVersion.replace("-SNAPSHOT", "-$it-SNAPSHOT") }
    .getOrElse(baseVersion)

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

// The javadoc jar is the published API documentation: exported packages only. Gradle hands
// javadoc every source file, so the internal package is excluded explicitly; the compiled
// classes are patched in so the API's references to internal types still resolve.
tasks.javadoc {
    exclude("org/jabref/chatpane/internal/**")
    val classesDirs = sourceSets.main.get().output.classesDirs
    options {
        this as StandardJavadocDocletOptions
        encoding = "UTF-8"
        addStringOption("-patch-module", "org.jabref.chatpane=${classesDirs.asPath}")
        addBooleanOption("Xdoclint:all,-missing", true)
        addBooleanOption("Werror", true)
    }
}

// Same publishing setup as html-to-node and JabRef's jablib (MADR 0012): snapshots land on
// https://central.sonatype.com/repository/maven-snapshots/, which JabRef's build already resolves.
mavenPublishing {
    configure(JavaLibrary(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = SourcesJar.Sources(),
    ))

    publishToMavenCentral()
    signAllPublications()

    coordinates("org.jabref", "chatpane", version.toString())

    pom {
        name = "ChatPane"
        description = "A JavaFX control that shows a chat conversation as bubbles, IRC lines or message by message"
        inceptionYear = "2026"
        url = "https://github.com/JabRef/chatpane/"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "jabref"
                name = "JabRef Developers"
                url = "https://github.com/JabRef/"
            }
        }
        scm {
            url = "https://github.com/JabRef/chatpane"
            connection = "scm:git:https://github.com/JabRef/chatpane"
            developerConnection = "scm:git:git@github.com:JabRef/chatpane.git"
        }
    }
}
