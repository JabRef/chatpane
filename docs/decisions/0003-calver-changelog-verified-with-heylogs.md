---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# CalVer Changelog, Verified with heylogs

## Context and Problem Statement

Session history lives in commit messages, which is fine for Claude sessions but unreadable as "what changed for the user".
The project ships continuously — pushes straight to `main`, no release planning yet — so a semantic version number would be invented, not earned.
How do we keep a human-readable change history that costs one bullet per push and cannot silently rot?

## Considered Options

* CalVer date sections (`YYYY-MM-DD`) in `CHANGELOG.md`, tagged `v<date>` at day end, format checked by [heylogs](https://github.com/nbbrd/heylogs)
* Keep a Changelog with SemVer versions and an `Unreleased` section
* Generated changelog from commit messages (git-cliff, release-please, Conventional Commits)

## Decision Outcome

Chosen option: **CalVer date sections checked by heylogs**.

A day is the natural release unit while work lands continuously.
The topmost section is the day in progress and links to `.../compare/v<previous date>...main` (the first one to `.../commits/main`); when the next day's first entry is written, the finished day is tagged `v<date>` and its link is rewritten to a tag-to-tag compare.
heylogs is the enforcement half: a Keep a Changelog linter with CalVer support, configured in `heylogs.properties`, run locally (`jbang heylogs@nbbrd check CHANGELOG.md`) and in CI with one rule set, without touching the Gradle build.

SemVer was rejected *for now*: a library API does eventually earn semantic versions, and the first Maven Central release is the moment to revisit this record.
Generated changelogs were rejected because commit bodies carry the session narrative, not user-facing summaries.

### Consequences

* Good, because the cost is one bullet per behavior-changing push, written by whoever made the change.
* Good, because heylogs makes the format a build failure rather than a review comment.
* Bad, because the day-rollover ritual (tag + rewrite one link) is manual; a miss is cheap, the stale link still points at `main`.
* Neutral, because jbang must be on PATH locally; CI installs it with `jbangdev/setup-jbang`.
