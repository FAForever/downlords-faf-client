/*
 * dllmain.c - BugSplat-proxy entry point (builds bugsplat_proxy.dll). This
 * gets loaded AS BugSplat.dll (real BugSplat.dll's 19 exports are forwarded
 * to a copy saved as BugSplat_real.dll via bugsplat_proxy.def). We
 * piggyback on the fact that ForgedAlliance.exe genuinely imports
 * BugSplat.dll from its own directory (confirmed via its import table),
 * which is what gets us loaded early, before the game's XACT audio engine
 * initializes.
 *
 * This file is now ONLY the injection vector - the actual guard-install
 * logic lives in xact_guards.c's StartXactGuardsWatcher(), shared with
 * guards_dllmain.c (which builds a standalone xact_guards.dll usable by
 * any OTHER loader, e.g. a debugger like FADeepProbe injecting it
 * directly instead of going through the BugSplat trick at all).
 */
#include <windows.h>
#include "xact_guards.h"

BOOL WINAPI DllMain(HINSTANCE hinst, DWORD reason, LPVOID reserved)
{
    (void)reserved;

    if (reason == DLL_PROCESS_ATTACH) {
        DisableThreadLibraryCalls(hinst);
        /* Don't do real work here - DllMain runs under the loader lock.
         * StartXactGuardsWatcher spawns a thread and does everything else
         * there (standard Win32 DLL-injection hygiene). */
        StartXactGuardsWatcher(hinst);
    }

    return TRUE;
}
