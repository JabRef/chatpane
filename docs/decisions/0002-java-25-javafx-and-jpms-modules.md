---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# Java 25, JavaFX 26, Named Modules, Gradle Kotlin DSL

## Context and Problem Statement

ChatPane is a JavaFX control library plus a demo application.
It must work in modular applications (the Java Module System) and in classpath applications alike.
Which Java and JavaFX versions does it target, how is it modularized, and how is it built?

## Considered Options

* Java 25 (LTS) with JavaFX 26, two named modules, Gradle Kotlin DSL with a convention plugin in `build-logic/`
* Java 21 (LTS) with JavaFX 21 for the widest audience
* Maven instead of Gradle
* Classpath-only jar with an `Automatic-Module-Name`

## Decision Outcome

Chosen option: **Java 25 with JavaFX 26, two named modules, Gradle Kotlin DSL**.

* Java 25 is the current LTS, gives Markdown doc comments (`///`, JEP 467) and unnamed variables (`_`), and matches the JDK installed here and in JabRef, the first likely consumer.
* JavaFX 26 runs on JDK 24+; tying the library to Java 21 would mean JavaFX 21 and forgo current skin/CSS fixes, for consumers that do not exist yet.
* `org.jabref.chatpane` (library) exports its API packages only and `requires transitive javafx.controls`, so a modular consumer needs one `requires`; `org.jabref.chatpane.demo` runs on the module path via `application { mainModule = … }` and thereby proves the module works.
  A real `module-info` rather than an `Automatic-Module-Name`: an automatic module cannot hide packages or declare `requires static`.
* Gradle Kotlin DSL; the JavaFX platform jars are chosen by patching platform variants onto the `org.openjfx` modules with `org.gradlex.jvm-dependency-conflict-resolution` (as JabRef does), because the `org.openjfx` Maven jars without classifier are empty.
  The shared setup (toolchain, JavaFX variants, test tasks) lives in one convention plugin, `build-logic/…/chatpane.java-conventions.gradle.kts`; versions in `gradle/libs.versions.toml`.
* Tests have no `module-info` and run on the class path ("whitebox"), so they can reach package-private helpers.

### Consequences

* Good, because the demo running on the module path is a permanent check of the module descriptor.
* Good, because one convention plugin keeps the two modules' builds from drifting.
* Bad, because consumers on Java 21 cannot use the library.
* Neutral, because publishing (Maven Central coordinates, POM without JavaFX classifiers) is not decided yet; group `org.jabref` is the natural choice (the library is built for JabRef).
