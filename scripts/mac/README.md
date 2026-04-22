# Running FAF on macOS (Apple Silicon)

These scripts run the FAF client natively on arm64 and launch Supreme
Commander: Forged Alliance through CrossOver's Wine under Apple's
Rosetta 2. Rosetta 2 provides a full software x87 FPU implementation,
giving IEEE 754 bit-identical single-precision results to x86
hardware, which is what keeps the lockstep simulation in sync with
other players.

## Requirements

- Apple Silicon Mac (M1 or later) running macOS 10.15 or later
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

# Build the client distribution for Apple Silicon
./gradlew installDist -PjavafxPlatform=mac-aarch64

# Drop a macOS faf-uid binary next to the JARs. The Linux binary
# downloaded by :downloadUnixUid during `installDist` will not run
# on macOS and must be overwritten. Official uid releases do not
# yet ship a macOS build, so we build one from FAForever/uid PR #17.
#
# IMPORTANT — if you're building from source, you also need the
# production RSA public key. The CMakeLists.txt ships with an
# example key (for testing); faf-uid built with it will be rejected
# by the lobby server. In the official release pipeline the real key
# is passed in via -DUID_PUBKEY_BYTES=... by a GitHub Actions secret.
# To build locally for your own use, pass the same flag with the
# production key bytes — otherwise lobby login fails.
#
# Once PR #17 is merged and a macOS binary is published to the
# official FAForever/uid releases, delete this whole block and let
# :downloadUnixUid fetch the real thing.
brew install cmake pkg-config cryptopp jsoncpp
git clone -b add-macos-support https://github.com/jfuruness/uid.git "$HOME/faf-mac/uid-src"
cmake -S "$HOME/faf-mac/uid-src" -B "$HOME/faf-mac/uid-src/build" \
      -DUID_PUBKEY_BYTES="$UID_PUBKEY_BYTES"   # production key, not the example
cmake --build "$HOME/faf-mac/uid-src/build"
cp "$HOME/faf-mac/uid-src/build/faf-uid" build/install/faf-client/
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
- The `faf-run.sh` launcher captures wine's stderr to
  `~/.faforever/logs/wine-<timestamp>.log` on every launch so abnormal
  exits can be diagnosed post-mortem.
