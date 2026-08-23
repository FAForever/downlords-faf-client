# Integration point in this repo

`GameBinariesUpdateTaskImpl.java` (`src/main/java/com/faforever/client/patch/`) is what
actually places `BugSplat.dll` (and its siblings) into `C:\ProgramData\FAForever\bin\` -
it copies them verbatim from the vanilla Steam SupCom install's own `bin` folder:

```java
static final Collection<String> BINARIES_TO_COPY = Arrays.asList(
    "BsSndRpt.exe", "BugSplat.dll", "BugSplatRc.dll", "DbgHelp.dll", ...
);
...
if (!Files.exists(destination)) {
    copy(source, destination, REPLACE_EXISTING);
}
```

Two things worth knowing about this:

1. **The client doesn't ship its own `BugSplat.dll`** - it's the real one, straight from
   Steam. That's the file this proxy is designed to shadow.
2. **The copy only happens if the destination doesn't already exist.** This is actually
   convenient for testing this fix manually (dropping the proxy in as `BugSplat.dll`
   won't get silently overwritten by a future client update), but it also means there's
   no existing "always place the latest version of this file" hook to attach to as-is.

## Where a real integration would go (not done here - this needs maintainer input)

The natural point would be a small addition to `copyGameFilesToFafBinDirectory()` (or a
new sibling method), executed *after* the existing binaries copy, that would:

1. Detect if the fix should be active (a preference toggle, most likely - this
   shouldn't be silently forced on everyone without opt-in given it's patching behavior
   around a Microsoft-owned DLL, not something in this codebase).
2. If so: rename/copy the just-placed real `BugSplat.dll` to `BugSplat_real.dll` (only
   if that file doesn't already exist, to avoid clobbering a real one with a stale proxy
   pass-through target on a repeat run), then place the built proxy DLL (bundled as a
   resource, the way `BsSndRpt.exe` etc. already ship) as `BugSplat.dll`.
3. Ideally, a version check against the on-disk `xactengine2_9.dll` (see the main
   README's portability section) before installing, so a mismatched Windows/DLL version
   fails safe (do nothing) instead of hooking the wrong byte offsets.

None of that is implemented here - deliberately. Wiring this into the actual
install/update path is a decision for the maintainers (does this belong in the client
at all vs. staying an optional community tool? what's the right opt-in mechanism?
who's responsible if a future `xactengine2_9.dll` update shifts the offsets?) - this
folder is meant to make that conversation concrete, not to presume the answer.
