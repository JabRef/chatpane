import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    `java-library`
    id("chatpane.java-conventions")
    alias(libs.plugins.maven.publish)
}

// SemVer (MADR 0012). -PversionSuffix=PR17 turns 0.1.0-SNAPSHOT into 0.1.0-PR17-SNAPSHOT, so a
// pull request snapshot is identifiable and does not clobber the one built from main.
version = "0.1.0" + (findProperty("versionSuffix")?.let { "-$it" } ?: "") + "-SNAPSHOT"

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

// Maven Central (MADR 0012), same setup as JabRef's jablib and html-to-node: snapshots land on
// https://central.sonatype.com/repository/maven-snapshots/; publishToMavenLocal needs no credentials.
mavenPublishing {
    configure(JavaLibrary(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = SourcesJar.Sources(),
    ))

    publishToMavenCentral()
    signAllPublications()

    coordinates("org.jabref", "chatpane", version.toString())

    pom {
        name = "chatpane"
        description = "A JavaFX control that shows a chat conversation as bubbles, IRC lines or Slack-style messages"
        inceptionYear = "2026"
        url = "https://github.com/JabRef/chatpane/"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://github.com/JabRef/chatpane/blob/main/LICENSE"
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
