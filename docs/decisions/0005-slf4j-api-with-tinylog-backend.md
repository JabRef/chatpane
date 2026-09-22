---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# SLF4J API, tinylog as the Backend

## Context and Problem Statement

The library needs to log (debug traces of layout changes, later warnings about bad input), and the demo needs to show those logs.
A library must not force a logging backend on its consumers: it runs inside their application, next to their logging setup.
An application might log through a backend's own API directly, but that choice does not carry over to a library.

## Considered Options

* SLF4J 2 API in library and demo; tinylog 2 as the backend (via `slf4j-tinylog`) in the demo and the tests only
* tinylog API directly
* `System.Logger` (JDK built-in), no dependency at all
* No logging in the library

## Decision Outcome

Chosen option: **SLF4J API, tinylog backend in the demo and tests**.

SLF4J is the de-facto facade of the Java ecosystem: every consumer's backend (Logback, Log4j 2, tinylog, JUL) already binds to it, so the library's messages land in the consumer's logs without configuration.
It is a real module (`org.slf4j`), and SLF4J 2 finds its provider through `ServiceLoader`, so on the module path the demo's backend is bound by service binding alone — nothing `requires` it.
tinylog is the backend because it is small, fast, and configured by one `tinylog.properties`.

Convention: `private static final Logger LOGGER = LoggerFactory.getLogger(X.class);`, parameterized messages (`{}`), no string concatenation.

`tinylog` directly was rejected because it would make the backend the consumer's problem.
`System.Logger` was the closest runner-up (zero dependencies), but its default backend is JUL, whose output few applications route anywhere, and its API has no parameterized messages with `{}`.

### Consequences

* Good, because consumers see the library's logs in whatever backend they use.
* Good, because the library's only runtime dependency beyond JavaFX is `slf4j-api` (MIT, ~70 KB).
* Bad, because a consumer without any SLF4J provider gets SLF4J's one-line "no providers" notice and no logs.
