# Building

Requires a 32-bit MinGW toolchain (the game and its DLLs are 32-bit, `i686-w64-mingw32-gcc`
specifically - a 64-bit-only MinGW will fail to link). Easiest path on Windows: install
[MSYS2](https://www.msys2.org/), then:

```
pacman -S mingw-w64-i686-gcc mingw-w64-i686-binutils mingw-w64-i686-headers mingw-w64-i686-crt
```

## 1. Fetch MinHook

This repo doesn't vendor [MinHook](https://github.com/TsudaKageyu/minhook) (BSD-2-Clause) -
fetch it into `src/minhook/`:

```bash
cd src
BASE="https://raw.githubusercontent.com/TsudaKageyu/minhook/master"
for f in src/buffer.c src/hde/hde32.c src/hde/hde64.c src/hook.c src/trampoline.c \
         include/MinHook.h src/buffer.h src/trampoline.h src/hde/hde32.h src/hde/hde64.h \
         src/hde/table32.h src/hde/table64.h src/hde/pstdint.h LICENSE.txt; do
  mkdir -p "minhook/$(dirname "$f")"
  curl -sfL "$BASE/$f" -o "minhook/$f"
done
```

## 2. Build

```bash
export PATH="/c/msys64/mingw32/bin:$PATH"   # adjust to your MSYS2 install location
cd src
gcc -shared -m32 -O2 -Wall \
  -o bugsplat_proxy.dll \
  dllmain.c proxylog.c logger.c debug_string_sink.c file_sink.c xact_guards.c flags.c \
  minhook/src/buffer.c minhook/src/hook.c minhook/src/trampoline.c \
  minhook/src/hde/hde32.c minhook/src/hde/hde64.c \
  bugsplat_proxy.def \
  -Iminhook/include -Iminhook/src \
  -lpsapi -mthreads
```

This produces `bugsplat_proxy.dll`, whose 19 exports all forward transparently to
whatever the real `BugSplat.dll` provides (see `bugsplat_proxy.def`) - see the main
README for the export-list-must-match-your-BugSplat.dll caveat.

### Optional: the standalone guards DLL

The same protection logic can also be built as a standalone DLL with no BugSplat
dependency at all - useful if something else (e.g. a debugger that already controls
the game process) wants to inject it directly instead of via the BugSplat trick:

```bash
gcc -shared -m32 -O2 -Wall \
  -o xact_guards.dll \
  guards_dllmain.c proxylog.c logger.c debug_string_sink.c file_sink.c xact_guards.c flags.c \
  minhook/src/buffer.c minhook/src/hook.c minhook/src/trampoline.c \
  minhook/src/hde/hde32.c minhook/src/hde/hde64.c \
  -Iminhook/include -Iminhook/src \
  -lpsapi -mthreads
```

No `.def` file needed - it doesn't forward any exports, it just installs the guards
via its own `DllMain` the moment it's loaded, by whatever loaded it.

## 3. Validate before touching a live install

Build and run `test_harness.c` the same way (`gcc -m32 -O2 -o test_harness.exe test_harness.c`)
from the same directory as a copy of the built `bugsplat_proxy.dll` and a copy of the real
`BugSplat.dll` renamed to `BugSplat_real.dll` - it loads both and confirms all hooks install
successfully against a real, directly-loaded `xactengine2_9.dll` without touching your actual
FAF installation. See the main README for what "pass" looks like.

## 4. Install (manual, until this ships through the FAF client itself)

In `C:\ProgramData\FAForever\bin\`:

1. **Back up the real `BugSplat.dll` first** - copy it aside before touching anything.
2. Copy the backed-up original to `BugSplat_real.dll` (the proxy's forwarder target).
3. Copy the built `bugsplat_proxy.dll` in as `BugSplat.dll`.

To undo: delete your `BugSplat.dll`, restore the backup you made in step 1 back to
`BugSplat.dll`, delete `BugSplat_real.dll`. Nothing else is touched.
