# Running FAF on macOS (Apple Silicon)

These scripts run the FAF client natively on arm64 and launch Supreme
Commander: Forged Alliance through Apple's Game Porting Toolkit, which
wraps a CrossOver-derived Wine under Rosetta 2. Rosetta 2 provides a
full software x87 FPU implementation, giving IEEE 754 bit-identical
single-precision results to x86 hardware — which is what keeps the
lockstep simulation in sync with other players.

## Requirements

- Apple Silicon Mac (M1 or later) running macOS 14 (Sonoma) or later
- Rosetta 2: `softwareupdate --install-rosetta --agree-to-license`
- [Homebrew](https://brew.sh)
- JDK 25 aarch64 ([Temurin](https://adoptium.net/temurin/releases/?version=25))
- A copy of Supreme Commander: Forged Alliance (Steam / GOG)

## One-time setup

```sh
# Wine that runs through Rosetta 2. GPTK's wine64 runs 32-bit PE
# binaries (like ForgedAlliance.exe) via wine32on64 automatically.
brew tap gcenx/wine
brew install --cask gcenx/wine/game-porting-toolkit
brew install winetricks

# A prefix for the game, plus the libraries it needs. WINE points
# winetricks at wine64 explicitly because GPTK does not ship a plain
# `wine` wrapper.
export WINEPREFIX="$HOME/faf-mac/wine-prefix"
export WINE="$(command -v wine64)"
wine64 wineboot --init
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
- GPTK is a sibling CrossOver fork from Apple, sharing the same
  wine32on64 32-bit shim and the same Rosetta-2 x87 FPU path as
  Gcenx's wine-crossover packaging. The one change here is which
  distribution supplies the Wine binary.
