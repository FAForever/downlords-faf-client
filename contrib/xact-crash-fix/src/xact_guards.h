#ifndef XACT_GUARDS_H
#define XACT_GUARDS_H
#include <windows.h>

/* Installs the process-wide VEH safety net plus the three targeted MinHook
 * guards at the known crash sites (+0x1D751 UAF, +0x1D212 null-deref,
 * +0x1DE46 list-walk - see the plan doc §2.7/§2.7b/§2.7c for the analysis
 * this is based on). Call once xactengine2_9.dll is confirmed loaded. */
void InstallXactGuards(HMODULE xactBase);

/* Convenience entry point for ANY loader (the BugSplat-proxy injection
 * vector, or in principle a debugger like FADeepProbe injecting this
 * module directly - see StartXactGuardsWatcher below) - spawns a
 * background thread that polls for xactengine2_9.dll and calls
 * InstallXactGuards() once it appears. Safe to call from DllMain (does
 * not do real work on the calling thread, respects the loader-lock rule).
 * `selfModule` is this module's own HMODULE, used only to init logging/
 * path resolution (ProxyLogInit) - pass whatever HMODULE DllMain received. */
void StartXactGuardsWatcher(HMODULE selfModule);

#endif
