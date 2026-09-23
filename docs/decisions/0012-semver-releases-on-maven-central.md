---
status: proposed
date: 2026-09-23
decision-makers: Oliver Kopp
---

# SemVer Releases on Maven Central

## Context and Problem Statement

JabRef, the first planned consumer, resolves its libraries from Maven Central and its snapshot repository.
MADR 0003 chose CalVer day sections while nothing was published and named the first Maven Central release as the moment to revisit that.
How do we publish the library, and which version numbers does it get?

## Considered Options

* SemVer, starting at `0.1.0`, with `-SNAPSHOT` builds from `main` and from pull requests; the vanniktech `maven-publish` plugin and workflows as in `JabRef/html-to-node`
* CalVer versions from the day tags of MADR 0003 (`v2026-09-22` → `2026.09.22`), each day tag a release
* JitPack builds from Git tags

## Decision Outcome

Chosen option: **SemVer from `0.1.0`, with snapshots**, as `org.jabref:chatpane`; supersedes MADR 0003 (heylogs stays, now checking SemVer).

`main` carries the next version as `X.Y.Z-SNAPSHOT`; every push publishes it to the Maven Central snapshot repository (`publish.yml`), a same-repository pull request as `X.Y.Z-PR<n>-SNAPSHOT`, so a change can be tried in JabRef before it is merged.
A release is a deliberate step: drop `-SNAPSHOT`, turn `[Unreleased]` into `[X.Y.Z]`, tag `vX.Y.Z` — `release.yml` publishes it and refuses a tagged `-SNAPSHOT`.
Only `:chatpane` is published; the demo is not.
The workflows are the ones from `JabRef/html-to-node`, with its organization secrets for signing and the Central portal.

CalVer was rejected: a release per day turns every day's work into a permanent, undeletable Maven Central version, and tells a consumer nothing about API breaks; snapshots already cover "try the latest".
JitPack was rejected: JabRef does not resolve from it, and it builds on demand with an environment we do not control.

### Consequences

* Good, because version numbers say whether an update can break a consumer.
* Good, because JabRef can test `main` or a pull request through snapshots without anything being released.
* Good, because the day-rollover tagging of MADR 0003 is gone.
* Bad, because a release is a manual commit and tag.
* Neutral, because `publishToMavenLocal` works without any credentials for local tries.
