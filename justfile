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

# The screen size is explicit: `xvfb-run`'s default is the distribution's
# (nixpkgs ships 640x480), too small for a test window.
#
# Run the TestFX UI tests (MADR 0008) on a virtual display.
[unix]
uitest:
    xvfb-run -a -s "-screen 0 1920x1200x24" ./gradlew uiTest

# Run the TestFX UI tests (MADR 0008) on the real desktop (Windows has no Xvfb).
[windows]
uitest:
    .\gradlew.bat uiTest

# Mechanical docs checks (CONSISTENCY.md).
consistency:
    bash scripts/consistency.sh
