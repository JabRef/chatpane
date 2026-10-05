# Developer command runner for ChatPane — https://just.systems
# `just` lists recipes; `just <name>` runs one. Each recipe has a [unix] and a
# [windows] variant where the gradle wrapper differs; `just --list` shows the
# comment of the variant for this OS, so both carry one. Windows recipes run under cmd.
set windows-shell := ["cmd.exe", "/c"]

# Show the recipe list when run without a recipe.
default:
    @just --list

# The pre-commit gate: compile, unit tests, requirement tracing, changelog format.
[unix]
check:
    ./gradlew build traceRequirements
    jbang heylogs@nbbrd check CHANGELOG.md

# The pre-commit gate: compile, unit tests, requirement tracing, changelog format.
[windows]
check:
    .\gradlew.bat build traceRequirements
    jbang heylogs@nbbrd check CHANGELOG.md

# Run the demo on the module path, e.g. `just demo --layout=bubbles`.
[unix]
demo *ARGS:
    ./gradlew :demo:run {{ if ARGS == "" { "" } else { "--args='" + ARGS + "'" } }}

# Run the demo on the module path, e.g. `just demo --layout=bubbles`.
[windows]
demo *ARGS:
    .\gradlew.bat :demo:run {{ if ARGS == "" { "" } else { "--args=\"" + ARGS + "\"" } }}

# Mechanical docs checks (CONSISTENCY.md).
consistency:
    bash scripts/consistency.sh
