/*
 * guards_dllmain.c - entry point for the STANDALONE guards DLL (builds
 * xact_guards.dll). Unlike bugsplat_proxy.dll, this has no need to
 * masquerade as anything or forward any exports - it's meant to be
 * injected directly by whatever already has a foothold in the game's
 * process, e.g. a debugger like FADeepProbe (which already launches
 * ForgedAlliance.exe under its own control - it could LoadLibrary this
 * DLL into the child process right after CreateProcess, as an
 * alternative to going through the BugSplat.dll proxy trick at all).
 *
 * This is intentionally the smallest possible file - all real logic is
 * shared with dllmain.c via xact_guards.c's StartXactGuardsWatcher().
 */
#include <windows.h>
#include "xact_guards.h"

BOOL WINAPI DllMain(HINSTANCE hinst, DWORD reason, LPVOID reserved)
{
    (void)reserved;

    if (reason == DLL_PROCESS_ATTACH) {
        DisableThreadLibraryCalls(hinst);
        StartXactGuardsWatcher(hinst);
    }

    return TRUE;
}
