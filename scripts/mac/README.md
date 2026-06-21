# Running FAF on macOS (Apple Silicon)

These scripts run the FAF client natively on arm64 and launch Supreme
Commander: Forged Alliance through CrossOver's Wine under Apple's
Rosetta 2. Rosetta 2 provides a full software x87 FPU implementation,
giving IEEE 754 bit-identical single-precision results to x86
hardware, which is what keeps the lockstep simulation in sync with
other players.

## Requirements

- Apple Silicon Mac (M1 or later) running macOS 14 (Sonoma) or later
- Rosetta 2: `softwareupdate --install-rosetta --agree-to-license`
- [Homebrew](https://brew.sh)
- JDK 25 aarch64 ([Temurin](https://adoptium.net/temurin/releases/?version=25))
- A copy of Supreme Commander: Forged Alliance (Steam / GOG)

## One-time setup

```sh
# Wine that runs through Rosetta 2. This is wine-crossover 23.7.1-1
# (wine-8.0.1, CrossOver's FOSS Wine) — the build this PR was
# soak-tested against (50+ multiplayer games, no desync).
# Mirrored at github.com/jfuruness/wine-crossover-mac because the
# upstream Gcenx/winecx repo and homebrew-wine cask were removed in
# early 2026.
WINECX_VER=23.7.1-1
curl -L -o /tmp/wine-crossover.tar.xz \
  "https://github.com/jfuruness/wine-crossover-mac/releases/download/${WINECX_VER}/wine-crossover-${WINECX_VER}-osx64.tar.xz"
sudo tar -xf /tmp/wine-crossover.tar.xz -C /Applications
sudo xattr -drs com.apple.quarantine "/Applications/Wine Crossover.app"
sudo codesign --force --deep -s - "/Applications/Wine Crossover.app"
export PATH="/Applications/Wine Crossover.app/Contents/Resources/wine/bin:$PATH"

brew install winetricks

# A prefix for the game, plus the libraries it needs
export WINEPREFIX="$HOME/faf-mac/wine-prefix"
wine64 wineboot --init
winetricks -q d3dx9 xact

# Build the client distribution for Apple Silicon. The :downloadUnixUid
# gradle task pulls the official faf-uid-macos arm64 binary from
# FAForever/uid v4.0.7 (which shipped macOS support after #17 merged)
# and stages it next to the JARs — no manual faf-uid build needed.
./gradlew installDist -PjavafxPlatform=mac-aarch64
```

Copy your Supreme Commander files into
`$WINEPREFIX/drive_c/games/SupremeCommander/` (the directory should
contain the `gamedata/` folder).

### Wine builds that do not work — do not use

Only **wine-crossover 23.7.1-1** is known to run SC:FA reliably.
The two other obvious candidates both have wine bugs that make the
game unplayable:

- **Apple's Game Porting Toolkit** (`gcenx/wine/game-porting-toolkit`,
  wine 7.7) — the game dies on startup with `Assertion failed:
  (end <= pages_vprot_size << pages_vprot_shift), function
  alloc_pages_vprot, file virtual.c, line 1032`. GPTK's
  `pages_vprot` page-protection tracking table is undersized for
  `ForgedAlliance.exe`'s memory layout.
- **Upstream WineHQ 11.0_1** (`Gcenx/macOS_Wine_builds`) — the game
  starts and plays but crashes mid-match with a non-zero exit code
  (observed exit 5 after ~20 min of multiplayer). This is a wine
  bug, not a setup issue. Maybe some debugging could get this to work,
  but I wouldn't count on it.

## Running

```sh
export JAVA_HOME=/path/to/jdk-25-aarch64
export WINEPREFIX="$HOME/faf-mac/wine-prefix"
./scripts/mac/faf-client.sh
```

In the client's settings, under **Forged Alliance Forever**, enter the
**fully-expanded** paths below — substitute your own macOS username for
`<you>`. The client uses Java's `Path.of()` to parse these, which does
not expand shell variables like `$WINEPREFIX` or `$HOME`:

| Setting                                    | Value                                                                                |
|--------------------------------------------|--------------------------------------------------------------------------------------|
| Forged Alliance install location           | `/Users/<you>/faf-mac/wine-prefix/drive_c/games/SupremeCommander`                     |
| Command line format for executable         | `/Users/<you>/path/to/downlords-faf-client/scripts/mac/faf-run.sh %s`                |
| Execution directory                        | `/Users/<you>/.faforever/bin`                                                        |

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
- The `faf-run.sh` launcher captures wine's stderr to
  `~/.faforever/logs/wine-<timestamp>.log` on every launch so abnormal
  exits can be diagnosed post-mortem.
