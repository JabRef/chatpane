---
status: accepted
date: 2026-09-23
decision-makers: Oliver Kopp
---

# Maven Central Publishing with CalVer Versions

## Context and Problem Statement

JabRef, the first planned consumer, resolves its libraries from Maven Central and its snapshot repository.
MADR 0003 names the first Maven Central release as the moment to revisit CalVer.
How do we publish the library, and which version numbers does it get?

## Considered Options

* The vanniktech `maven-publish` plugin as in JabRef's jablib and `JabRef/html-to-node`, CalVer versions from the day tags
* The same plugin with SemVer versions
* JitPack builds from Git tags

## Decision Outcome

Chosen option: **vanniktech `maven-publish`, CalVer versions from the day tags**, as `org.jabref:chatpane`.

Every push to `main` publishes `main-SNAPSHOT` to the Maven Central snapshot repository, a same-repository pull request `pr<n>-SNAPSHOT` (`publish.yml`).
Every day tag `v<YYYY-MM-DD>` releases that day as `YYYY.MM.DD` (`release.yml`); dots, because tools read a dash as the start of a pre-release qualifier.
Only `:chatpane` is published; the demo is not.
The workflows are the ones from `JabRef/html-to-node`, with its organization secrets for signing and the Central portal.

SemVer was rejected for now: the changelog's day sections already are the release units, and a second version scheme next to them would drift.
JitPack was rejected: JabRef does not resolve from it, and it builds on demand with an environment we do not control.

### Consequences

* Good, because JabRef can depend on a released version, or on `main-SNAPSHOT` while both evolve.
* Good, because the day-rollover tag of MADR 0003 is the release — no extra step.
* Bad, because Maven Central releases cannot be deleted: a day tag pushed on a broken commit is a broken release forever.
* Neutral, because `publishToMavenLocal` works without any credentials for local tries.
