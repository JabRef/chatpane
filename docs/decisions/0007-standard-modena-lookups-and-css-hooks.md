---
status: accepted
date: 2026-09-22
decision-makers: Carl Christian Snethlage
---

# No Palette of Its Own: Standard Modena Lookups and CSS Hooks

## Context and Problem Statement

The pane lives inside other people's applications and has to fit their theme — Modena, a dark Modena variant, AtlantaFX, JabRef's own themes — and follow a live theme switch.
Carl: "The control should not use a theming layout directly, but should provide CSS hooks following standard conventions. Should be droppable into any application and already look fine as JavaFX standard style classes are used."
Where do the pane's colors come from, and how does an application change them?

## Considered Options

* No palette of its own: the user-agent stylesheet uses only the standard Modena lookups (`-fx-accent`, `-fx-base`, `-fx-background`, `-fx-control-inner-background`, `-fx-text-background-color`, `-fx-mid-text-color`) and standard nodes (`list-view`, `list-cell`, `label`); restyling through documented style classes and pseudo-classes
* An own set of looked-up colors (`-cp-bubble-outgoing`, …) derived from Modena's, overridden by the application
* Styleable `ObjectProperty<Paint>` properties on the control (one per color)
* Depend on a theme library (AtlantaFX) and use its `-color-*` variables

## Decision Outcome

Chosen option: **no palette of its own, standard lookups and CSS hooks**.

Every JavaFX theme defines Modena's lookups, because every built-in control is styled through them; a control that uses only those looks native wherever it is dropped in, with no theme-specific code and no setup.
Text contrast follows Modena's own mechanism: a container sets `-fx-background` (a bubble: `-fx-base`, an outgoing bubble: `-fx-accent`) and its labels take `-fx-text-background-color`, which Modena ladders against that background.
The stylesheet is a user-agent stylesheet (`Control.getUserAgentStylesheet()`, MADR 0009), so any application stylesheet wins without `!important`.

The hooks follow the JavaFX convention — style classes for structure, pseudo-classes for state — and are listed in the README's *CSS reference*: `.chat-pane` with `:bubbles`/`:irc`/`:modern`; `.message-cell` (a `list-cell`) with `:incoming`/`:outgoing`/`:continued`; `.message-bubble`, `.irc-line`, `.modern-entry`, `.message-header`; `.message-sender`, `.message-time`, `.message-text` (all `label`s).
An application recolors outgoing bubbles with `.chat-pane .message-cell:outgoing .message-bubble { -fx-background: …; }` — the same way it would restyle a selected list cell.

An own palette was rejected because it is a theming layer of its own: every application would have to learn and map a second set of names.
`Paint` properties were rejected as verbose for no gain over CSS, and a theme library dependency because it would force a theme on every consumer.

Rules:
* No color literal in Java code — no `Color.web`, no hex string, no color in `setStyle`; the skin assigns style classes and pseudo-classes, CSS decides the color.
* No color literal in `chatpane.css` either; only standard lookups.
* Sizes in `em`, so the pane follows the application's font size.
* Exceptions: colors that are data (e.g. a future per-sender color supplied by the application) — passed through, never invented in code.

### Consequences

* Good, because the pane looks native in Modena and every theme built on its lookups, including a live theme switch.
* Good, because restyling needs no knowledge beyond ordinary JavaFX CSS.
* Bad, because a theme that does not define Modena's lookups (rare; a full replacement of Modena) leaves the pane with JavaFX's fallbacks.
