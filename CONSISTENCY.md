# Consistency checks

What must agree with what, and how to find out when it stopped agreeing.
The build catches the code-side drift (`traceRequirements`, heylogs, the tests); this file covers the drift the build cannot see: prose that describes code, and prose that describes other prose.

Run: `scripts/consistency.sh` first (the mechanical checks; one line per finding, exit 1 if any), then walk the judgment checks below.

## Mechanical (`scripts/consistency.sh`)

1. **Spec ids in living prose exist at the cited revision.**
   A `dsn~x~3` in README.md or the requirement documents must be defined in `docs/requirements/` at exactly `~3`.
   Exempt as history: `PLAN.md`, `CHANGELOG.md`, and the MADRs.
2. **Relative links resolve.** Every relative Markdown link in README.md, PLAN.md and `docs/` points at a file that exists.
3. **MADRs.** Every `MADR NNNN` cited in code or docs has a file; every decision file is listed in `docs/decisions/README.md`.
4. **`just` recipes** named in README.md, AGENTS.md and `docs/` exist in the `justfile`.
5. **CSS hooks are documented.** Every style class and pseudo-class the library's Java code sets (`getStyleClass().add…("…")`, `PseudoClass.getPseudoClass("…")`) appears in the README's *CSS reference*.

6. **Workarounds.** Every `Workaround W<n>` marker in code, build files or CSS has a `## W<n>` entry in `docs/workarounds.md`, and every entry has at least one marker.

## Judgment (a reader with the code open)

7. **README usage compiles.** The Java snippets in README.md match the current API (constructors, property names).
8. **Requirement text matches the tagged code.** For each `dsn` item touched since the last run (`git log --since` on `docs/requirements/`), read the classes carrying its `[impl->dsn~…]` tag and confirm the item still describes what they do.
9. **AGENTS.md conventions are followed.** Spot-check what a build cannot enforce: `///` doc comments, `@NullMarked` in every `package-info.java`, SLF4J only (no tinylog import in `chatpane/src/main`), no color literal in `chatpane/src/main` (Java or CSS), one sentence per line in edited Markdown.
10. **CHANGELOG and PLAN.md agree with the code.** The topmost CHANGELOG section describes features that exist; the milestone checklist in PLAN.md is not ahead of or behind the implementation.
11. **README's CSS reference has nothing stale.** Check 5 finds hooks missing from the README, not README rows whose hook the code no longer sets (the `.irc-line` row outlived its code by one commit).
12. **Workarounds are still needed.** For each `docs/workarounds.md` entry, follow its "Removable when" check against the current upstream version.

## Adding a check

A check earns a place here when its drift was found by hand at least once.
Mechanical if a grep can decide it: add it to `scripts/consistency.sh` and a line above.
Judgment otherwise: a line above naming the two things that must agree and where each lives.
