# Supreme Commander: FA — Crash Containment Fix

A community-buildable fix for a long-standing crash in Forged Alliance / FAF: the game randomly hard-crashes to desktop during play, most often several minutes into a match, sometimes on exit. This document explains the bug, the fix, what's confirmed vs. still open, and what would need to change before this could be shared or upstreamed.

**Architecture note (current, `src/` reflects this):** the fix is split into two pieces — `xact_guards.c`/`flags.c` contain all the actual protection logic (shared, reusable), and each of `dllmain.c` (builds `bugsplat_proxy.dll`, the injection vector used today) / `guards_dllmain.c` (builds a standalone `xact_guards.dll`, usable by any OTHER loader — e.g. a debugger like [FADeepProbe](https://github.com/faforever/fadeepprobe) injecting it directly, without needing the BugSplat trick at all) is just a thin entry point calling into the shared code. Diagnostic output goes through `OutputDebugStringA` by default (visible to any attached debugger); pass `/logxactguards` on the game's own command line to also get a persistent log file next to wherever the DLL loaded from. See `INTEGRATION.md` for the concrete integration point found in this repo (`GameBinariesUpdateTaskImpl.java`).

## The bug, in plain terms

The game's audio system occasionally crashes the whole process. This isn't a driver problem, a hardware problem, or anything specific to one machine — it's a bug in a small piece of code Microsoft shipped in 2007 as part of the DirectX runtime, which this ~2007-era game engine depends on and which Microsoft never updated. Two different game threads can end up touching the same piece of audio memory at the same moment — one thread is in the middle of freeing/reloading it, another thread tries to read it — and whichever one loses that race crashes the entire game.

## The bug, technically

**Component:** `xactengine2_9.dll`, version `9.20.1057.0000 (WGGT_AUG07.Release)` — Microsoft's XACT Engine API, part of the DirectX End-User Runtime (August 2007 release), `Microsoft Corporation`. This is a redistributable component installed once by any DirectX-9-era game that needs it (not a Windows OS file that varies by Windows version) and lives at `C:\Windows\SysWOW64\xactengine2_9.dll` on a 64-bit Windows install.

**Three known fault sites**, all in the same subsystem (wave-bank reload/free, driven by one function `FUN_0041d60e`):

| Offset | Function | Bug shape |
|---|---|---|
| `+0x1D751` | `FUN_0041d751` (teardown) | Use-after-free: a pointer (`this+0x5c`) is null-checked but the *pointee* was already freed by another thread. Reads Windows' freed-heap fill pattern `0xFEEEFEEE`, then crashes calling through it. |
| `+0x1D212` | `FUN_0041d212` (cue lookup) | Null-pointer dereference: a 4-hop pointer chain off the same field has no null check between hops; if a reload is mid-flight, an intermediate hop is null and the next dereference faults. |
| `+0x1DE46` | (unnamed, linked-list walk) | Iterator invalidation: walks a linked list of wave-bank nodes, calling the same reload function on each node — but that call can free/mutate nodes later in the list, which the walk never accounts for. |

**Root cause** (confirmed via Ghidra static analysis, whole-binary cross-reference scan for every write to the shared field): the reload function `FUN_0041d60e` correctly frees and nulls the shared pointer, and correctly guards *itself* against re-entering concurrently via `InterlockedCompareExchange` on two flag fields. But **none of the three reader sites above check or acquire that same lock** — they just read the raw pointer. This is a classic partial/asymmetric locking bug: the writer locks against itself, never against readers, and the readers were never written to participate in the lock at all. The null-deref and use-after-free crashes are two symptoms of the exact same missing synchronization, not two separate defects; the third (list-walk) site is a related but mechanically distinct bug in the same subsystem.

This matches a years-old, well-known community report (independent of this investigation) of the identical crash — same DLL, same version (`9.20.1057.0`), same exception code — which is itself useful portability evidence: this isn't a one-machine problem.

## How the fix works

**We don't patch `xactengine2_9.dll` itself.** It's a Microsoft DLL, and while it's plain unsigned userland code (not kernel, no anti-tamper observed), directly binary-patching a shared system file is invasive and hard to distribute/undo cleanly. Instead:

1. **Injection vector:** `ForgedAlliance.exe` genuinely imports `BugSplat.dll` (FAF's own bundled crash-reporter) from its own install directory by name — normal Windows DLL search order checks the application's own directory first, so a same-named DLL placed there loads *instead of* the real one. We built a **proxy `BugSplat.dll`** that transparently forwards all 19 of the real DLL's exports (verified byte-for-byte against the original — nothing about BugSplat's own behavior changes) and additionally runs our own code at load time. `xactengine2_9.dll` itself can't be shadowed this same way — it's COM-activated (resolved via the registry, not search order) — so BugSplat is purely a "way to get our own code running early inside the game's process," unrelated to what BugSplat itself does.
2. **Hooking:** once loaded, a background thread polls for `xactengine2_9.dll` to appear (it loads a moment later, when the game initializes audio), then uses **MinHook** (github.com/TsudaKageyu/minhook, BSD-2-Clause) to install inline hooks at the three known fault-site addresses, computed as `(loaded module base) + (fixed offset)` — so it's independent of ASLR/where Windows happens to load the DLL each run.
3. **Containment:** each hook wraps the real function call using `setjmp`/`longjmp` triggered from inside a Windows Vectored Exception Handler (GCC/MinGW has no `__try`/`__except` — that's an MSVC-only extension — so this is the portable equivalent). If the wrapped call faults, the VEH recognizes it's inside a guarded call on that thread (thread-local state, so it's race-safe with itself) and jumps back to right after the `setjmp`, turning "the whole game crashes" into "that one risky operation was skipped, logged, and play continues." A process-wide VEH also catches (and logs, but does **not** attempt to recover from) any *other* access violation inside the DLL that isn't one of the three known sites — deliberately conservative: we don't guess at recovery for faults we haven't characterized, since a wrong guess could corrupt state worse than crashing.
4. **Feature flags:** each of the three hooks can be set to `disabled` / `passthrough` (installed but no recovery, still logged) / `contain` (full recovery) via a plain-text config file, re-read once per game launch — no rebuild needed to try different combinations.

## Confirmed vs. still open

**Confirmed, with real evidence (not just theory):**
- All three hooks install successfully against the real DLL (standalone test harness, `MH_OK` for every `CreateHook`/`EnableHook` call).
- The wrapper is live and has caught real crashes during actual play: the `+0x1DE46` site fired and was contained **4 times in about 5 minutes** in one session, with the game continuing to run each time (confirmed via the runtime log and the live process still running afterward). Before this fix, that exact fault would have killed the game every time.

**Still open / not yet done:**
- The root-cause fix (adding the missing lock acquisition at the three reader sites, rather than catching the resulting crash) has not been attempted — containment was judged sufficient and much lower-risk for now.
- Haven't confirmed via a live debugger (WinDbg) which specific caller reaches the reader sites from a background thread — inferred from static evidence (the asymmetric-locking pattern plus prior worker-thread crash stack frames), not directly observed together in one trace.
- Dr. Memory (installed, ready) hasn't been run against a real play session yet — would give the first fully dynamic confirmation (real free-call-stack + real bad-reuse-call-stack) of the static analysis above.
- Whether there are *more* unguarded fault sites in the same subsystem beyond these three is unknown — they were found reactively (one from initial static analysis, two more from live crashes as they occurred), not from an exhaustive audit.

## Portability: will this work on someone else's machine?

**Short answer: probably yes for the core mechanism, but not as currently packaged — three concrete things need fixing first.**

1. **Hardcoded paths (must fix before sharing with anyone).** The log file and the feature-flag config file are currently hardcoded to this machine's specific path: `C:\Users\admin\Desktop\supreme commander crash debug\...` (see `proxylog.c` line 12, `flags.c` line 7). This will not work on any other machine as-is — needs to become relative to the DLL's own install directory (e.g. write next to `BugSplat.dll` itself) or a standard location like `%LOCALAPPDATA%\FAForever\`.

2. **`xactengine2_9.dll` version dependency — reasonably strong evidence it's fine, not proven.** The three hook addresses are fixed byte offsets into this specific DLL. This machine's copy is version `9.20.1057.0000 (WGGT_AUG07.Release)`. Reasons to be fairly confident this is the same file on most other Windows installs that have this game working at all:
   - It's a DirectX End-User Runtime redistributable component, not a Windows OS file — it doesn't change with Windows updates, and Microsoft never released a newer version of it after 2007.
   - The independent community crash report referenced above (a different user, found in earlier research, not affiliated with this session) hit the identical DLL version and the identical exception signature — real cross-machine evidence, not just theory.
   - That said, this has not been verified against a second real machine in this session. **Before wide distribution, the safe move is to have the proxy DLL check the loaded `xactengine2_9.dll`'s version at runtime and refuse to install the hooks (falling back to pure pass-through / logging only) if the version doesn't match exactly** — cheap insurance against a version mismatch silently hooking the wrong bytes and crashing somewhere new instead of fixing anything.

3. **`BugSplat.dll` export list dependency.** The 19-export forwarder table is pinned to whatever `BugSplat.dll` FAF's client currently bundles. If FAF ever updates to a different BugSplat SDK version with a different export set, the forwarder `.def` file would need regenerating (a mismatched/missing forward is a hard DLL-load failure, so this fails loudly, not silently — not dangerous, just needs a rebuild when it happens).

4. **Platform scope:** Windows-only by nature (Win32 DLL injection, PE format, MinHook). No reason to expect issues across different Windows 10/11 builds specifically, since nothing here depends on Windows-version-specific behavior — only on the DirectX component above. Works on 32-bit or 64-bit Windows host the same way (the game itself is always 32-bit, running under WOW64 on a 64-bit host, which is what this was built/tested on).

## Checklist before this could be shared or upstreamed

- [ ] Replace hardcoded log/config paths with something portable (relative to the DLL's own directory, or a standard per-user app-data location).
- [ ] Add a runtime version check against the real `xactengine2_9.dll` before installing hooks; fail safe (pass-through only) on a mismatch rather than assuming the offsets are still correct.
- [ ] Determine FAF's actual license (not yet checked — don't assume GPL or anything else) before writing any license header into code intended for submission.
- [ ] Decide on distribution mechanism (see below) and, if pursuing upstream, open a conversation with FAF maintainers before assuming a PR would be accepted — this patches behavior around a Microsoft-owned DLL, not FAF's own code, so it's a judgment call for them, not something a PR can force through.
- [ ] Consider running under Dr. Memory for one real play session to get dynamic confirmation of the root-cause analysis, strengthening confidence before wider release.
- [ ] Decide whether to also fix the root cause (add the missing lock at the reader sites) rather than only containing the crash — lower risk to ship containment first, but worth revisiting once more confidence is built up.

## Where this could be hosted / shared

A few realistic options, in rough order of fit:

- **FAF's own client repo (`FAForever/downlords-faf-client`), as a bundled binary asset.** This is the most "real" path if the goal is it becoming a standard part of FAF: the client already bundles binary assets (`BugSplat.dll`, `dbghelp.dll`) alongside `ForgedAlliance.exe` at install/update time, so there's precedent for shipping a native DLL this way even though the client itself is Java. Requires maintainer buy-in, not just a PR — see checklist above.
- **FAF community channels (Discord, forums) as a standalone optional download.** Lowest-friction way to get it in front of the people who'd actually use it and get feedback, without needing anyone's approval first. A GitHub repo of your own (or a Gist) as the actual file host, linked from there, is the standard pattern for this kind of community tool.
- **A dedicated small GitHub repo of your own.** Gives it a real home with version history, issues, and a README — reasonable middle ground between "just a forum post" and "merged into the official client." Also the natural staging point if an eventual PR to the client repo is the long-term goal (point the client at a tagged release).
- **ModDB (moddb.com).** Not a great fit — ModDB is built around game *content* mods (maps, factions, total conversions, textures), not binary patches/crash fixes for the underlying engine. Nothing stops someone from posting it there, but it's not really the audience or format ModDB is designed for, and it would likely get more attention and better context in an FAF-specific venue instead.

No public action was taken as part of preparing this document — no repo, branch, or PR was created, nothing was uploaded anywhere. This is purely groundwork for your own review before deciding how (or whether) to share this further.
