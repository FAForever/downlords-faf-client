# Running FAF on macOS (Apple Silicon)

These scripts run the FAF client natively on arm64 and launch Supreme
Commander: Forged Alliance through a CrossOver-derived Wine under
Apple's Rosetta 2. Rosetta 2 provides a full software x87 FPU
implementation, giving IEEE 754 bit-identical single-precision results
to x86 hardware — which is what keeps the lockstep simulation in sync
with other players.

## Known issues (work in progress)

- **Apple's Game Porting Toolkit (`gcenx/wine/game-porting-toolkit`) does
  not work for SC:FA.** GPTK 3.0-2 ships wine-7.7, whose
  `pages_vprot` page-protection tracking table is undersized for the
  `ForgedAlliance.exe` memory layout. The game dies on startup with:
  `Assertion failed: (end <= pages_vprot_size << pages_vprot_shift),
  function alloc_pages_vprot, file virtual.c, line 1032`. This is why
  the install steps below point at WineHQ's mac builds instead.
- **Intermittent mid-game crash with WineHQ 11.0_1** — game has been
  observed to exit with code 5 after ~20 minutes of play. Cause
  not yet diagnosed; the wine launcher (`faf-run.sh`) now writes wine's
  stderr to `~/.faforever/logs/wine-*.log` so the next occurrence can
  be captured.
- **wine-crossover 23.7.1-1** (wine-8.0.1, the build with 50+ games of
  stable soak-testing in the original PR) is no longer publicly
  distributable — the `Gcenx/winecx` repo and the `wine-crossover`
  Homebrew cask were both removed in early 2026. If you have the
  tarball cached locally from a prior install, you can use it; see
  "Alternative: wine-crossover" below.

## Requirements

- Apple Silicon Mac (M1 or later) running macOS 10.15 or later
- Rosetta 2: `softwareupdate --install-rosetta --agree-to-license`
- [Homebrew](https://brew.sh)
- JDK 25 aarch64 ([Temurin](https://adoptium.net/temurin/releases/?version=25))
- A copy of Supreme Commander: Forged Alliance (Steam / GOG)

## One-time setup

```sh
# Wine that runs through Rosetta 2. Gcenx packages upstream WineHQ's
# macOS builds as a GitHub release; they include wine32on64 and are
# new enough (wine 11.0) to avoid the GPTK alloc_pages_vprot bug.
WINE_VER=11.0_1
curl -L -o /tmp/wine-stable.tar.xz \
  "https://github.com/Gcenx/macOS_Wine_builds/releases/download/${WINE_VER}/wine-stable-${WINE_VER}-osx64.tar.xz"
sudo tar -xf /tmp/wine-stable.tar.xz -C /Applications
sudo xattr -drs com.apple.quarantine "/Applications/Wine Stable.app"
sudo codesign --force --deep -s - "/Applications/Wine Stable.app"
export PATH="/Applications/Wine Stable.app/Contents/Resources/wine/bin:$PATH"

brew install winetricks

# A prefix for the game, plus the libraries it needs
export WINEPREFIX="$HOME/faf-mac/wine-prefix"
wine wineboot --init
winetricks -q d3dx9 xact

# Build the client distribution for Apple Silicon
./gradlew installDist -PjavafxPlatform=mac-aarch64

# Drop a macOS faf-uid binary next to the JARs. The Linux binary
# downloaded by :downloadUnixUid during `installDist` will not run
# on macOS and must be overwritten. Official uid releases do not
# yet ship a macOS build, so we build one from FAForever/uid PR #17.
#
# Once that PR is merged and a macOS binary is published to the
# official FAForever/uid releases, delete this whole block and let
# :downloadUnixUid fetch the real thing.
brew install cmake pkg-config cryptopp jsoncpp
git clone -b add-macos-support https://github.com/jfuruness/uid.git "$HOME/faf-mac/uid-src"
cmake -S "$HOME/faf-mac/uid-src" -B "$HOME/faf-mac/uid-src/build"
cmake --build "$HOME/faf-mac/uid-src/build"
cp "$HOME/faf-mac/uid-src/build/faf-uid" build/install/faf-client/
```

Copy your Supreme Commander files into
`$WINEPREFIX/drive_c/games/SupremeCommander/` (the directory should
contain the `gamedata/` folder).

### Alternative: wine-crossover

If you already have `wine-crossover-23.7.1-1-osx64.tar.xz` cached from
a previous install (typically at
`~/Library/Caches/Homebrew/downloads/*wine-crossover*`), you can use it
instead of WineHQ's build — that's what the original PR's soak-testing
was done against. The setup is the same shape; replace the `curl`/`tar`
block above with:

```sh
tar -xf ~/Library/Caches/Homebrew/downloads/*wine-crossover-23.7.1-1-osx64.tar.xz -C /Applications
sudo xattr -drs com.apple.quarantine "/Applications/Wine Crossover.app"
sudo codesign --force --deep -s - "/Applications/Wine Crossover.app"
export PATH="/Applications/Wine Crossover.app/Contents/Resources/wine/bin:$PATH"
```

`faf-run.sh` auto-detects both `Wine Stable.app` and `Wine Crossover.app`
and picks whichever is installed.

## Running

```sh
export JAVA_HOME=/path/to/jdk-25-aarch64
export WINEPREFIX="$HOME/faf-mac/wine-prefix"
./scripts/mac/faf-client.sh
```

In the client's settings, under **Forged Alliance Forever**:

| Setting                                    | Value                                                            |
|--------------------------------------------|------------------------------------------------------------------|
| Forged Alliance install location           | `$WINEPREFIX/drive_c/games/SupremeCommander`                     |
| Command line format for executable         | `/absolute/path/to/scripts/mac/faf-run.sh %s`                    |
| Execution directory                        | `$HOME/.faforever/bin`                                           |

Do not add leading spaces or extra quotes to the command line format —
the `%s` placeholder is already quoted by the client.

## Notes

- Steam and Discord integration are not available on macOS; their native
  libraries have no arm64 builds. The `mac` Spring profile skips them.
- Rosetta 2 adds roughly 20–30% CPU overhead on the simulation thread.
  Lower graphics settings (Options → Video) if you need more headroom.
- Game resolution must be changed while the game is fully closed by
  editing `primary_adapter` inside the `profiles → options` block of
  `$WINEPREFIX/drive_c/users/<username>/AppData/Local/Gas Powered Games/Supreme Commander Forged Alliance/Game.prefs`.
  Changing it in-game freezes Wine because it cannot switch display
  modes mid-session.
