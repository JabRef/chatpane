---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# Requirements Tracing with OpenFastTrace

## Context and Problem Statement

The control's feature set (layouts, theming, later Markdown bodies, message actions, input line) grows in small bursts.
Scope drift and forgotten requirements are the main risks, and a library's API makes every half-finished promise visible to its users.
How are requirements captured and connected to code?

## Considered Options

* OpenFastTrace (OFT): spec items in Markdown, coverage tags in code, tracing in the build
* GitHub issues only
* Requirements documents without tracing

## Decision Outcome

Chosen option: "OpenFastTrace", because the `feat → req → dsn → impl/utest` chain makes untouched scope visible mechanically (`gradlew traceRequirements` fails on defects), the artifacts are plain Markdown in the repo, and the Gradle plugin (`org.itsallcode.openfasttrace`) integrates it into CI.

Details: `docs/requirements/features.md` holds vision-level `feat~` items; per-area files hold `req~` (user stories) and `dsn~` (design) items; code carries `[impl->dsn~…~1]` / `[utest->dsn~…~1]` tags.
Spec items are introduced in the same commit as their first covering code, so the trace stays green throughout.

### Consequences

* Good, because "what is missing" is a build report, not archaeology.
* Good, because requirements survive suspend/resume gaps verbatim.
* Bad, because every feature commit touches docs + code + tags — friction by design.
* Bad, because tracing is syntactic, not semantic: linking requirements to code does not guarantee that the code works or does what it should do.
* Neutral, because OFT does not scan `.css` files; a design implemented by a stylesheet is tagged at the Java code that loads it.
