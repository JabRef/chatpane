---
status: accepted
date: 2026-10-05
decision-makers: Carl Christian Snethlage
---

# Headless UI Tests in `build`

## Context and Problem Statement

Much of a control's behavior only exists once it is laid out: cells are created by the list view's virtual flow, pseudo-classes and CSS apply in a scene.
Unit tests cover the pure logic (grouping), but how is the rendered control tested?

This record first (2026-09-22) chose TestFX in a separate `uiTest` task that needed a display — the developer's desktop or Xvfb — and was therefore left out of `build`.
JavaFX 25 added a headless glass platform of its own, and JabRef moved its JavaFX tests to it and dropped TestFX (JabRef PR 16850, 2026-09-08); this revision follows.

## Considered Options

* JavaFX's headless platform (`glass.platform=Headless`, software rendering), a small JUnit extension of our own, UI tests in `test`
* The headless platform, TestFX kept, UI tests still in a separate task
* TestFX in a separate `uiTest` task on a display or Xvfb (the first choice)
* Monocle headless platform
* Manual checks with the demo only

## Decision Outcome

Chosen option: **JavaFX's headless platform, our own JUnit extension, UI tests in `test`**.

`FxTestApplication` (a class annotation naming the test's `Application`) and `FxTestExtension` (after JabRef's `JavaFxExtension`) start the toolkit once per test JVM with `glass.platform=Headless` and `prism.order=sw`, give the application a fresh `Stage` before the class and hide its windows after it.
An exception thrown on the FX thread during a test fails that test.
The Gradle `test` task sets the same properties; a `-Dglass.platform=…` given to an IDE's test JVM still wins, to watch a test on the desktop.
So `build` — the pre-commit gate and CI's `build` job — runs every UI test; no Xvfb, no separate CI job, no window on the developer's desktop.

Our tests never needed TestFX's robot: they query nodes and fire events, and `FxThread` (`onFx`, `settle`) already did the thread work.
Dropping TestFX also drops its EUPL-1.2 (weak copyleft) test dependency.
Monocle, rejected before for lagging JavaFX releases, is superseded by the platform JavaFX ships itself.

Rules: query nodes rather than drive a robot; sort cells by `getIndex()` — the virtual flow keeps recycled cells in any order; the test JVM runs with `java.awt.headless=true` so no test starts a real browser.

### Consequences

* Good, because every `build` checks rendered behavior (layouts, CSS-settable properties) — the skin can no longer break between deliberate `uiTest` runs.
* Good, because no display, no Xvfb, and one test dependency less.
* Bad, because software rendering on a headless platform is not what users see: GPU pipelines, window managers and HiDPI scaling are untested — the demo, checked in both themes, still covers those by eye.
* Neutral, because `build` takes a few seconds longer (all UI tests ran in about 7 s on the development machine).
