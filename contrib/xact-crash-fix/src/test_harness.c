/*
 * test_harness.c - standalone validation, run entirely from build/, never
 * touches the live FAF directory. Tests EITHER injection vector - pass
 * "bugsplat" (default) to test bugsplat_proxy.dll (the BugSplat.dll
 * forwarder trick) or "guards" to test xact_guards.dll directly (the
 * standalone path, usable by any other loader e.g. a debugger).
 *
 * Since diagnostic output now goes through OutputDebugStringA instead of
 * a log file, this harness implements the standard "DBWIN" debug-string
 * capture protocol (the same one Sysinternals DebugView uses) so we can
 * actually see and verify the output without needing DebugView installed.
 *
 * "Pass" = both LoadLibrary calls succeed, and the captured debug output
 * shows MH_OK (0) for both MH_CreateHook/MH_EnableHook at all three
 * offsets. This does NOT exercise the actual crash-recovery (setjmp/
 * longjmp) path - that needs the real XACT object's vtables active via a
 * real game session.
 */
#include <windows.h>
#include <stdio.h>
#include <string.h>
#include <process.h>

static HANDLE g_bufferReady, g_dataReady, g_mapping;
static void *g_view;
static volatile BOOL g_capturing = TRUE;

static unsigned __stdcall DbgStringCaptureThread(void *param)
{
    (void)param;
    while (g_capturing) {
        DWORD wait = WaitForSingleObject(g_dataReady, 200);
        if (wait == WAIT_OBJECT_0) {
            DWORD pid = *(DWORD *)g_view;
            const char *text = (const char *)g_view + sizeof(DWORD);
            printf("[dbgstr pid=%lu] %s", (unsigned long)pid, text);
            SetEvent(g_bufferReady);
        }
    }
    return 0;
}

static HANDLE StartDbgStringCapture(void)
{
    g_bufferReady = CreateEventA(NULL, FALSE, TRUE, "DBWIN_BUFFER_READY");
    g_dataReady   = CreateEventA(NULL, FALSE, FALSE, "DBWIN_DATA_READY");
    g_mapping     = CreateFileMappingA(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE, 0, 4096, "DBWIN_BUFFER");
    g_view        = MapViewOfFile(g_mapping, FILE_MAP_READ, 0, 0, 512);

    if (!g_bufferReady || !g_dataReady || !g_mapping || !g_view) {
        printf("[harness] WARNING: couldn't set up debug-string capture (GetLastError=%lu) - "
               "hooks may still work, you just won't see their log output here\n",
               (unsigned long)GetLastError());
        return NULL;
    }

    uintptr_t h = _beginthreadex(NULL, 0, DbgStringCaptureThread, NULL, 0, NULL);
    return (HANDLE)h;
}

int main(int argc, char **argv)
{
    const char *which = (argc > 1) ? argv[1] : "bugsplat";
    const wchar_t *dllName = (strcmp(which, "guards") == 0) ? L"xact_guards.dll" : L"bugsplat_proxy.dll";

    printf("[harness] testing: %ls\n", dllName);
    printf("[harness] starting debug-string capture (so we can see OutputDebugStringA output)...\n");
    HANDLE captureThread = StartDbgStringCapture();

    printf("[harness] loading %ls...\n", dllName);
    HMODULE proxy = LoadLibraryW(dllName);
    if (!proxy) {
        printf("[harness] FAIL: LoadLibraryW(%ls) failed, GetLastError=%lu\n",
               dllName, (unsigned long)GetLastError());
        if (wcscmp(dllName, L"bugsplat_proxy.dll") == 0) {
            printf("[harness] (most likely cause: one of the 19 export forwarders in "
                   "bugsplat_proxy.def didn't resolve - check BugSplat_real.dll is present "
                   "in this directory)\n");
        }
        return 1;
    }
    printf("[harness] OK: %ls loaded, handle=%p\n", dllName, proxy);

    printf("[harness] loading the REAL system xactengine2_9.dll directly (simulates COM activation)...\n");
    HMODULE xact = LoadLibraryW(L"C:\\Windows\\SysWOW64\\xactengine2_9.dll");
    if (!xact) {
        printf("[harness] FAIL: LoadLibraryW(real xactengine2_9.dll) failed, GetLastError=%lu\n",
               (unsigned long)GetLastError());
        return 1;
    }
    printf("[harness] OK: real xactengine2_9.dll loaded at %p\n", xact);

    printf("[harness] waiting 3s for the watcher thread to detect it and install guards...\n");
    Sleep(3000);

    printf("[harness] done - check the [dbgstr] lines above for \"MH_EnableHook\" -> 0 (0 = MH_OK) "
           "at all three offsets (+0x1D751, +0x1D212, +0x1DE46).\n");

    g_capturing = FALSE;
    if (captureThread) { WaitForSingleObject(captureThread, 1000); CloseHandle(captureThread); }

    FreeLibrary(xact);
    FreeLibrary(proxy);
    return 0;
}
