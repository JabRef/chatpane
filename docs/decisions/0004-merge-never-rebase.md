---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# Merge, Never Rebase

## Context and Problem Statement

Work happens in one git worktree per task (workspace `CLAUDE.md`), several Claude sessions may run at once, and all of them push straight to `main` — there are no pull requests.
How does a session integrate `main` into its branch, and how does it recover from a rejected push?

## Considered Options

* Always merge (`git merge origin/main`, `git pull --no-rebase`)
* Rebase onto `origin/main` before pushing, for a linear history
* Squash each task into one commit on `main`

## Decision Outcome

Chosen option: **always merge**.

A rebase rewrites commits another session may already have fetched, merged or built on; a merge commit keeps each session's state visible and never invalidates what someone else has seen.
Squashing loses the commit bodies, which carry the session narrative.

### Consequences

* Good, because concurrent sessions never lose or duplicate each other's work.
* Good, because the history shows which session did what, in which worktree.
* Bad, because the history is not linear; `git log --first-parent` gives the linear view of `main`.
