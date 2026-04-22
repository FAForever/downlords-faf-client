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

# Prefer an explicit WINE_BIN, then auto-detect. WineHQ's macOS build
# ships `wine` (which handles 32-bit PE via wine32on64); wine-crossover
# ships both `wine` and `wine64`. Either works for ForgedAlliance.exe.
WINE_CANDIDATES=(
    "${WINE_BIN:-}"
    "/Applications/Wine Stable.app/Contents/Resources/wine/bin/wine"
    "/Applications/Wine Crossover.app/Contents/Resources/wine/bin/wine64"
    "$(command -v wine || true)"
    "$(command -v wine64 || true)"
)
WINE_BIN=""
for candidate in "${WINE_CANDIDATES[@]}"; do
    if [ -n "$candidate" ] && [ -x "$candidate" ]; then
        WINE_BIN="$candidate"
        break
    fi
done
if [ -z "$WINE_BIN" ]; then
    echo "Wine not found. See scripts/mac/README.md for install instructions." >&2
    exit 1
fi

# Capture wine's stderr to a timestamped log so abnormal exits (non-zero
# code with empty game log) can be diagnosed post-mortem.
WINE_LOG_DIR="$HOME/.faforever/logs"
mkdir -p "$WINE_LOG_DIR"
WINE_LOG="$WINE_LOG_DIR/wine-$(date +%Y%m%d-%H%M%S).log"
exec "$WINE_BIN" "$@" 2> "$WINE_LOG"
