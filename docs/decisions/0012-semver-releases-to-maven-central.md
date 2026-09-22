---
status: accepted
date: 2026-09-23
decision-makers: Carl Christian Snethlage
---

# SemVer Releases to Maven Central

## Context and Problem Statement

JabRef is to consume ChatPane as a Maven artifact, so the library needs coordinates, a version scheme and a release pipeline.
[MADR 0003](0003-calver-changelog-verified-with-heylogs.md) chose daily CalVer sections and deferred the version scheme "to the first Maven Central release" — this is that moment.
Central releases are immutable and public, and consumers resolve them with version ranges and update tools that assume "higher number, compatible unless the major changes".
How are releases numbered, triggered and published?

## Considered Options

* SemVer (`0.1.0`), a release per pushed `v<version>` tag, snapshots from `main` and pull requests — the setup of JabRef's [html-to-node](https://github.com/JabRef/html-to-node)
* CalVer as Maven version (`2026.09.23`), daily tags kept, a release only when a maintainer runs the release workflow by hand
* CalVer as Maven version, every daily tag published to Central

## Decision Outcome

Chosen option: **SemVer with tag-triggered releases, as in html-to-node**, because a library API is what SemVer describes, and JabRef's other published libraries work the same way, so one set of maintainers, secrets and habits covers all of them.

* Coordinates `org.jabref:chatpane` (MADR 0002 named `org.jabref` as the natural group); only `:chatpane` is published, not the demo.
* Publishing with the `com.vanniktech.maven.publish` Gradle plugin: signed jar, sources and javadoc (exported packages only), POM with Apache-2.0.
  JavaFX stays an ordinary `api` dependency without classifier; consumers pick the platform jars as they already do for JavaFX itself.
* The version lives in `gradle.properties` (`chatpaneVersion`), `-SNAPSHOT` between releases.
* `publish.yml` publishes `main` as `<version>-SNAPSHOT` and each same-repository pull request as `<version>-PR<n>-SNAPSHOT` to Central's snapshot repository, so JabRef can try a branch before it merges.
* `release.yml` publishes and releases on a pushed `v<version>` tag, and refuses when the tag and `chatpaneVersion` disagree.
* The changelog collects changes under *Unreleased*; a release renames that section to the version, heylogs checks with `versioning=semver`.
  The daily `v<date>` tags stop; none had been pushed.

### Consequences

* Good, because consumers get compatibility promises from the version number, and Central receives only deliberate releases.
* Good, because pull-request snapshots let JabRef integrate a change before it lands.
* Bad, because a release takes two pull requests (set the version and date the changelog section; then back to the next `-SNAPSHOT`), since `main` takes no direct pushes.
* Bad, because signing and Central credentials must be repository secrets that only maintainers can set; the workflows use the same secret names as html-to-node.
* Neutral, because while at `0.x`, SemVer allows breaking changes in minor versions, which fits an API that is still settling.

## More Information

Supersedes [MADR 0003](0003-calver-changelog-verified-with-heylogs.md) in its versioning; heylogs as the changelog linter stays.
