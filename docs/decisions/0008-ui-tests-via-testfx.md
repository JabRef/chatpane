---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# UI Tests via TestFX, Outside `build`

## Context and Problem Statement

Much of a control's behavior only exists once it is laid out: cells are created by the list view's virtual flow, pseudo-classes and CSS apply in a scene.
Unit tests cover the pure logic (grouping), but how is the rendered control tested?

## Considered Options

* TestFX (the maintained fx-labs fork, `io.gitlab.fx-labs:testfx-junit`) in a separate `uiTest` task, tagged `@Tag("ui")`
* TestFX tests inside `test`, run by every `build`
* Monocle headless platform for all UI tests
* Manual checks with the demo only

## Decision Outcome

Chosen option: **TestFX in a separate `uiTest` task**.

UI tests need a display (a desktop, or Xvfb on Linux via `just uitest`); keeping them out of `build` keeps the pre-commit gate fast and green on a headless CI runner.
The fx-labs fork is maintained, supports current JavaFX, and uses AssertJ; it is EUPL-1.2 (weak copyleft), acceptable because it is a test-scope dependency only.
Monocle was rejected for now: it lags JavaFX releases and renders differently from a real pipeline.

Rules: prefer queries over robot input (tests run on the developer's desktop); sort cells by `getIndex()` — the virtual flow keeps recycled cells in any order; the `uiTest` JVM runs with `java.awt.headless=true` so no test starts a real browser.

### Consequences

* Good, because rendered behavior (layouts, CSS-settable properties) is checked mechanically.
* Bad, because `uiTest` is not part of the gate and must be run deliberately when touching the skin or CSS.
