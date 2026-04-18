# Running FAF on macOS (Apple Silicon)

These scripts run the FAF client natively on arm64 and launch Supreme
Commander: Forged Alliance through `wine-crossover`, which in turn runs
through Apple's Rosetta 2. Rosetta 2 provides a full software x87 FPU
implementation, giving IEEE 754 bit-identical single-precision results
to x86 hardware — which is what keeps the lockstep simulation in sync
with other players.

## Requirements

- Apple Silicon Mac (M1 or later) running macOS 13 or later
- Rosetta 2: `softwareupdate --install-rosetta --agree-to-license`
- [Homebrew](https://brew.sh)
- JDK 25 aarch64 ([Temurin](https://adoptium.net/temurin/releases/?version=25))
- A copy of Supreme Commander: Forged Alliance (Steam / GOG)

## One-time setup

```sh
# Wine that runs through Rosetta 2
brew tap gcenx/wine
brew install --cask --no-quarantine gcenx/wine/wine-crossover
brew install winetricks

# A prefix for the game, plus the libraries it needs
export WINEPREFIX="$HOME/faf-mac/wine-prefix"
wineboot --init
winetricks -q d3dx9 xact

# Build the client distribution for Apple Silicon
./gradlew installDist -PjavafxPlatform=mac-aarch64

# Drop a macOS faf-uid binary next to the JARs
# (official builds: https://github.com/FAForever/uid/releases)
cp /path/to/faf-uid build/install/faf-client/
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
