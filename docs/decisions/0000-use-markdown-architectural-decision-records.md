---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# Use Markdown Architectural Decision Records

## Context and Problem Statement

ChatPane is a one-person project worked on in bursts, largely by Claude Code sessions, with gaps of days or months between them.
Design decisions and their rationale must survive these gaps, otherwise every resumption re-litigates old choices.
A library adds a second reader: someone deciding whether to depend on it wants to know why it looks the way it does.

## Considered Options

* MADR (Markdown Architectural Decision Records)
* Free-form design notes in the README
* No records, decisions only in Git history and issues

## Decision Outcome

Chosen option: "MADR", because decisions are versioned with the code and the template forces the "considered options" and "consequences" sections that make resumption cheap.
Records live in `docs/decisions/NNNN-title.md` using the current MADR template and are listed in `docs/decisions/README.md`.

### Consequences

* Good, because a fresh session (human or AI) can reconstruct why the architecture looks the way it does.
* Good, because superseding a decision is explicit (status change + new record) instead of silent drift.
* Bad, because writing a proper record takes effort for each significant decision.
