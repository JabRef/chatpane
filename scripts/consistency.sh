#!/usr/bin/env bash
# Mechanical half of CONSISTENCY.md: prints one line per finding, exits 1 if any.
# Run from anywhere; the judgment checks stay in CONSISTENCY.md.
set -u
cd "$(dirname "$0")/.."
finding() { echo "$1: $2"; }

checks() {

defined=$(mktemp)
grep -rhoE '^`(feat|req|dsn)~[a-z0-9-]+~[0-9]+`' docs/requirements/*.md | tr -d '`' | sort -u > "$defined"

# 1. Spec ids cited in living prose exist at that revision.
# PLAN.md, CHANGELOG.md and the MADRs are history: they cite the revision current when written.
for f in README.md docs/requirements/*.md; do
  grep -noE '`?(feat|req|dsn)~[a-z0-9-]+~[0-9]+' "$f" | tr -d '`' | sort -u | while IFS=: read -r line id; do
    grep -qx "$id" "$defined" || finding "stale spec id" "$f:$line $id"
  done
done

# 2. Relative Markdown links resolve.
for f in README.md PLAN.md docs/requirements/*.md docs/decisions/*.md; do
  d=$(dirname "$f")
  grep -oE '\]\([^)#: ]+(#[^)]*)?\)' "$f" | sed -E 's/^\]\(//; s/\)$//; s/#.*//' | sort -u | while read -r l; do
    [ -n "$l" ] && [ ! -e "$d/$l" ] && finding "broken link" "$f -> $l"
  done
done

# 3. MADR numbers cited anywhere exist, and every MADR file is in the decisions index.
grep -rhoE 'MADR [0-9]{4}' --include=*.java --include=*.md --include=*.kts --include=*.css chatpane demo build-logic docs README.md AGENTS.md PLAN.md 2>/dev/null \
  | sort -u | while read -r m; do
  n=${m#MADR }
  ls docs/decisions/"$n"-*.md >/dev/null 2>&1 || finding "MADR cited but missing" "$m"
done
for f in docs/decisions/[0-9]*.md; do
  grep -q "$(basename "$f")" docs/decisions/README.md || finding "MADR not in index" "$f"
done

# 4. `just` recipes the docs name exist.
grep -ohE '`just [a-z-]+' README.md AGENTS.md docs/*/*.md 2>/dev/null | grep -oE '[a-z-]+$' | sort -u | while read -r r; do
  grep -qE "^$r( |:)" justfile || finding "docs name an unknown just recipe" "$r"
done

# 5. Every style class and pseudo-class the library sets is in the README's CSS reference.
css_ref=$(awk '/^## CSS reference/,/^## Demo/' README.md)
grep -rhoE '(getStyleClass\(\)\.addAll?\(|getPseudoClass\()[^)]*' chatpane/src/main/java \
  | grep -oE '"[a-z-]+"' | tr -d '"' | sort -u | while read -r hook; do
  echo "$css_ref" | grep -qE "[.:]$hook\b" || finding "CSS hook missing from README" "$hook"
done
# Pseudo-classes named after the layouts are built from MessageLayout, not literals.
for layout in $(grep -oE '^    [A-Z]+[,;]' chatpane/src/main/java/org/jabref/chatpane/MessageLayout.java | tr -d ' ,;' | tr 'A-Z' 'a-z'); do
  echo "$css_ref" | grep -q ":$layout" || finding "layout pseudo-class missing from README" "$layout"
done

# 6. Workaround markers and docs/workarounds.md entries match, both ways.
markers=$(grep -rhoE 'Workaround W[0-9]+' chatpane/src demo/src build-logic/src build.gradle.kts settings.gradle.kts 2>/dev/null \
  | grep -oE 'W[0-9]+' | sort -u)
entries=$(grep -oE '^## W[0-9]+' docs/workarounds.md | grep -oE 'W[0-9]+' | sort -u)
comm -23 <(echo "$markers") <(echo "$entries") | while read -r w; do [ -n "$w" ] && finding "workaround marker without entry" "$w"; done
comm -13 <(echo "$markers") <(echo "$entries") | while read -r w; do [ -n "$w" ] && finding "workaround entry without marker" "$w"; done

rm -f "$defined"
}

out=$(checks)
[ -z "$out" ] && exit 0
echo "$out"
exit 1
