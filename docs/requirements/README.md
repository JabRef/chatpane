# Requirements

Requirements are traced with [OpenFastTrace](https://github.com/itsallcode/openfasttrace) (see [decision 0001](../decisions/0001-requirements-tracing-with-openfasttrace.md)).

## Structure

| File | Content |
|------|---------|
| [features.md](features.md) | Vision-level features (`feat~…`) distilled from the [README](../../README.md) |
| [chat-pane.md](chat-pane.md) | The control, its message model, skin and stylesheet (`req~` + `dsn~`) |
| [demo.md](demo.md) | The demo application (`req~` + `dsn~`) |

Chain: `feat` → `req` (user stories) → `dsn` (design) → `[impl->dsn~…~1]` / `[utest->dsn~…~1]` tags in source code.

## Conventions

* A `dsn` item gains `Needs: impl` (and `utest` where unit-testable logic exists) **in the same commit** that adds the covering code and tags. This keeps `gradlew traceRequirements` green at every commit.
* Spec item names are kebab-case and stable; increase the revision on semantic change and update all coverage tags in the same commit.
* OpenFastTrace does not scan `.css` files: a design implemented by the stylesheet is tagged where Java code loads it (`ChatPane.getUserAgentStylesheet`).
* A UI test covering a design carries a `[utest->dsn~…]` tag like a unit test.

## Running the trace

```
gradlew traceRequirements
```

Report: `build/reports/tracing.txt`. The build fails on any tracing defect.
