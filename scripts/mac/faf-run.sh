#!/bin/bash
#
# Supreme Commander: Forged Alliance launcher via Wine (Rosetta 2).
#
# Set as the "Command line format for executable" in the FAF client:
#   /path/to/scripts/mac/faf-run.sh %s
#
# WINEPREFIX must point at the prefix containing the installed game.
#

set -eu

: "${WINEPREFIX:?WINEPREFIX must be set to the Wine prefix containing Supreme Commander}"
export WINEPREFIX
export WINEDEBUG="${WINEDEBUG:--all}"

WINE_BIN="${WINE_BIN:-$(command -v wine64 || command -v wine || true)}"
if [ -z "$WINE_BIN" ]; then
    echo "Wine not found. Install: brew install --cask gcenx/wine/game-porting-toolkit" >&2
    exit 1
fi

exec "$WINE_BIN" "$@"
