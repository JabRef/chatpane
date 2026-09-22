---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# JSpecify for Nullness

## Context and Problem Statement

A library's API must say which parameters and return values may be `null`; the JavaFX API itself says little about it.
How is nullness expressed?

## Considered Options

* JSpecify: `@NullMarked` on every package, `@Nullable` where null is allowed
* JetBrains `@Nullable`/`@NotNull`
* Javadoc prose only

## Decision Outcome

Chosen option: **JSpecify**, because it is the cross-tool standard (IntelliJ, NullAway, Kotlin, Checker Framework read it) and `@NullMarked` per package makes non-null the default, so only the exceptions need an annotation.

* Every `package-info.java` carries `@NullMarked`; nullable types are annotated in type-use position (`@Nullable ChatMessage`, `Outer.@Nullable Inner`).
* The module `requires static org.jspecify` and the build declares it `compileOnlyApi`: not needed at run time, visible to consumers compiling against the API.
* Public JavaFX properties can be set to `null` through binding despite a non-null contract; getters that matter normalize (`ChatPane.getMessageLayout()` reads `null` as the default).

### Consequences

* Good, because nullness is part of the API that tools and consumers can check.
* Bad, because nothing enforces it in the build yet; NullAway (Error Prone) is a later option.
