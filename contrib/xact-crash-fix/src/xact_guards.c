/*
 * xact_guards.c
 *
 * Crash containment for three known access-violation sites inside the real
 * xactengine2_9.dll (Microsoft's ~2007 XACT audio middleware, statically
 * analyzed via Ghidra - see the plan doc's §2.7/§2.7a/§2.7b/§2.7c for the
 * full writeup). All three trace back to the same underlying subsystem
 * (wave-bank reload/free, `FUN_0041d60e`), reached via the object field
 * this+0x5c or a related linked-list of the same objects:
 *
 *   +0x1D212 (FUN_0041d212): a pointer-chain hop (this->0x5c->0x2c) can be
 *     NULL and gets dereferenced with no check - "Read from 0x00000010"
 *     class of crash. Root cause (§2.7b): FUN_0041d60e frees+nulls
 *     this->0x5c under its own InterlockedCompareExchange lock, but this
 *     reader never checks/acquires that lock - classic missing-lock race.
 *
 *   +0x1D751 (FUN_0041d751): a teardown routine null-checks this->0x5c
 *     before a vtable call, but the *pointee* can already be freed
 *     (dangling, not NULL) - reads Windows' freed-heap fill pattern
 *     0xFEEEFEEE. Same missing-lock race as above, different fault flavor.
 *
 *   +0x1DE46 (linked-list walk, §2.7c): walks a sentinel-terminated linked
 *     list, calling FUN_0041d60e on every node - i.e. a self-modifying
 *     traversal that never protects itself against FUN_0041d60e freeing
 *     list entries out from under the walk. Related subsystem, different
 *     bug shape (iterator invalidation, not the reader/writer race).
 *
 * GCC/MinGW does not implement MSVC's __try/__except language extension,
 * so containment here uses the standard setjmp/longjmp-from-a-Vectored-
 * Exception-Handler pattern instead: each detour sets a per-thread
 * recovery point before calling the real (trampolined) function; if that
 * call faults, the VEH below recognizes we're inside a guarded call on
 * this thread and longjmp()s back, turning the fault into "that one call
 * didn't happen" instead of a process crash. A small per-thread stack of
 * recovery points (not just one) handles the fact that +0x1D751's own
 * body internally calls +0x1D212 - MinHook's inline hook redirects that
 * internal call through our detour too, so guarded calls can nest.
 *
 * Strategy pattern: each site's runtime behavior (disabled / passthrough /
 * contain, see flags.h) is read from an external config file once at
 * install time, so different combinations can be tried across launches
 * without rebuilding - see flags.c.
 *
 * This is a genuinely unusual pattern - deliberately over-commented so a
 * future reader (including future us) doesn't need to reverse-engineer
 * the reasoning again.
 */
#include <windows.h>
#include <psapi.h>
#include <process.h>
#include <setjmp.h>
#include "MinHook.h"
#include "proxylog.h"
#include "flags.h"
#include "xact_guards.h"

#define MAX_GUARD_DEPTH 8

/* One entry per currently-active guarded call on this thread. Grouping the
 * recovery point with its site name (instead of two parallel arrays) makes
 * "these belong to the same stack frame" the type itself, not a convention
 * you have to remember to keep in sync. */
typedef struct {
    jmp_buf recoveryPoint;
    const char *siteName;
} GuardFrame;

static __thread GuardFrame g_guardFrames[MAX_GUARD_DEPTH];
static __thread int        g_activeGuardCount = 0;

static void  *g_xactBase = NULL;
static SIZE_T g_xactSize = 0;
static PVOID  g_vehHandle = NULL;

/* ---- +0x1D751: use-after-free / dangling-pointer teardown site ---- */

typedef void (__thiscall *TeardownFn)(void *this_);
static TeardownFn s_realTeardownFn = NULL;

static void __thiscall TeardownDetour_0x1D751(void *this_)
{
    if (g_flags.hook1D751 == STRAT_PASSTHROUGH) {
        s_realTeardownFn(this_);
        return;
    }

    if (g_activeGuardCount >= MAX_GUARD_DEPTH) {
        ProxyLog("hook-1d751: guard stack full (depth=%d), calling through UNGUARDED, this=%p",
                 g_activeGuardCount, this_);
        s_realTeardownFn(this_);
        return;
    }

    int depth = g_activeGuardCount++;
    g_guardFrames[depth].siteName = "1d751(UAF-teardown)";

    if (setjmp(g_guardFrames[depth].recoveryPoint) == 0) {
        s_realTeardownFn(this_);
    } else {
        ProxyLog("hook-1d751: CONTAINED an access violation inside the teardown/UAF site "
                 "(this=%p, depth=%d) - one wave-bank cleanup call was skipped, game should continue",
                 this_, depth);
    }

    g_activeGuardCount--;
}

/* ---- +0x1D212: null pointer-chain hop, dominant repeat crash ---- */

typedef void (__thiscall *NullHopFn)(void *this_, unsigned char param1);
static NullHopFn s_realNullHopFn = NULL;

static void __thiscall NullHopDetour_0x1D212(void *this_, unsigned char param1)
{
    if (g_flags.hook1D212 == STRAT_PASSTHROUGH) {
        s_realNullHopFn(this_, param1);
        return;
    }

    if (g_activeGuardCount >= MAX_GUARD_DEPTH) {
        ProxyLog("hook-1d212: guard stack full (depth=%d), calling through UNGUARDED, this=%p param1=%u",
                 g_activeGuardCount, this_, (unsigned)param1);
        s_realNullHopFn(this_, param1);
        return;
    }

    int depth = g_activeGuardCount++;
    g_guardFrames[depth].siteName = "1d212(null-hop)";

    if (setjmp(g_guardFrames[depth].recoveryPoint) == 0) {
        s_realNullHopFn(this_, param1);
    } else {
        ProxyLog("hook-1d212: CONTAINED an access violation on the null-pointer-chain site "
                 "(this=%p, param1=%u, depth=%d) - that sound/cue lookup was skipped, game should continue",
                 this_, (unsigned)param1, depth);
    }

    g_activeGuardCount--;
}

/* ---- +0x1DE46: linked-list iterator invalidation, §2.7c ---- */

typedef void (__thiscall *ListWalkFn)(void *this_);
static ListWalkFn s_realListWalkFn = NULL;

static void __thiscall ListWalkDetour_0x1DE46(void *this_)
{
    if (g_flags.hook1DE46 == STRAT_PASSTHROUGH) {
        s_realListWalkFn(this_);
        return;
    }

    if (g_activeGuardCount >= MAX_GUARD_DEPTH) {
        ProxyLog("hook-1de46: guard stack full (depth=%d), calling through UNGUARDED, this=%p",
                 g_activeGuardCount, this_);
        s_realListWalkFn(this_);
        return;
    }

    int depth = g_activeGuardCount++;
    g_guardFrames[depth].siteName = "1de46(list-walk)";

    if (setjmp(g_guardFrames[depth].recoveryPoint) == 0) {
        s_realListWalkFn(this_);
    } else {
        /* A fault mid-walk means we abandon the rest of the list for this
         * call - the nodes already processed before the fault keep
         * whatever FUN_0041d60e already did to them; the ones not yet
         * reached just don't get reloaded this pass. Same "skip the risky
         * bit, keep playing" tradeoff as the other two sites. */
        ProxyLog("hook-1de46: CONTAINED an access violation mid-list-walk "
                 "(this=%p, depth=%d) - remainder of that wave-bank reload pass was skipped, game should continue",
                 this_, depth);
    }

    g_activeGuardCount--;
}

/* ---- process-wide safety net ---- */

static LONG WINAPI HandleXactAccessViolation(EXCEPTION_POINTERS *exceptionInfo)
{
    if (exceptionInfo->ExceptionRecord->ExceptionCode != EXCEPTION_ACCESS_VIOLATION) {
        return EXCEPTION_CONTINUE_SEARCH;
    }

    void *faultAddr = (void *)(UINT_PTR)exceptionInfo->ContextRecord->Eip;
    BOOL inXactModule = g_xactBase &&
        (BYTE *)faultAddr >= (BYTE *)g_xactBase &&
        (BYTE *)faultAddr <  (BYTE *)g_xactBase + g_xactSize;

    if (g_activeGuardCount > 0) {
        /* We're inside one of our own guarded calls on THIS thread (TLS -
         * naturally scoped per-thread, so a fault on a different thread
         * can never mistakenly recover here even if that thread also has
         * a guard active). Recover at the innermost active guard. */
        int depth = g_activeGuardCount - 1;
        ProxyLog("VEH: access violation at %p during guarded call [%s] (depth=%d) - recovering via longjmp",
                 faultAddr, g_guardFrames[depth].siteName, depth);
        longjmp(g_guardFrames[depth].recoveryPoint, 1); /* does not return */
    }

    if (inXactModule) {
        /* A fault inside xactengine2_9.dll that ISN'T inside one of our
         * guarded calls (either a genuinely new/unknown site, or a
         * PASSTHROUGH-strategy site deliberately not catching). We
         * deliberately do NOT attempt recovery here - we don't understand
         * every possible fault well enough to know what a safe
         * continuation looks like, and a wrong guess could corrupt state
         * worse than just crashing. Log full context so this becomes a
         * new candidate for a targeted hook, then let it crash normally
         * (so WER/the game's own crash handler still produces a report). */
        ProxyLog("VEH: UNGUARDED access violation inside xactengine2_9.dll at %p "
                 "(module base=%p size=%lu) - Eax=%08lx Ecx=%08lx Edx=%08lx Ebx=%08lx "
                 "Esp=%08lx Ebp=%08lx Esi=%08lx Edi=%08lx - NOT contained, letting it crash "
                 "so this becomes a new hook candidate",
                 faultAddr, g_xactBase, (unsigned long)g_xactSize,
                 exceptionInfo->ContextRecord->Eax, exceptionInfo->ContextRecord->Ecx,
                 exceptionInfo->ContextRecord->Edx, exceptionInfo->ContextRecord->Ebx,
                 exceptionInfo->ContextRecord->Esp, exceptionInfo->ContextRecord->Ebp,
                 exceptionInfo->ContextRecord->Esi, exceptionInfo->ContextRecord->Edi);
    }

    return EXCEPTION_CONTINUE_SEARCH;
}

static void InstallOneHook(const char *siteLabel, void *target, void *detour, void **originalFnOut, GuardStrategy strategy)
{
    if (strategy == STRAT_DISABLED) {
        ProxyLog("InstallXactGuards: %s DISABLED by config, not installing hook (target=%p)", siteLabel, target);
        return;
    }

    MH_STATUS hookStatus = MH_CreateHook(target, detour, originalFnOut);
    ProxyLog("InstallXactGuards: MH_CreateHook %s (target=%p, strategy=%s) -> %d",
             siteLabel, target, strategy == STRAT_PASSTHROUGH ? "passthrough" : "contain", (int)hookStatus);
    if (hookStatus == MH_OK) {
        hookStatus = MH_EnableHook(target);
        ProxyLog("InstallXactGuards: MH_EnableHook %s -> %d", siteLabel, (int)hookStatus);
    }
}

void InstallXactGuards(HMODULE xactBase)
{
    LoadXactFlags();

    MODULEINFO moduleInfo;
    if (GetModuleInformation(GetCurrentProcess(), xactBase, &moduleInfo, sizeof(moduleInfo))) {
        g_xactBase = moduleInfo.lpBaseOfDll;
        g_xactSize = moduleInfo.SizeOfImage;
    } else {
        /* Fallback: base is what we were handed, size unknown -> treat as
         * a single point rather than a range for the "inXactModule" check. */
        g_xactBase = xactBase;
        g_xactSize = 1;
    }

    ProxyLog("InstallXactGuards: xactengine2_9.dll base=%p size=%lu",
             g_xactBase, (unsigned long)g_xactSize);

    g_vehHandle = AddVectoredExceptionHandler(1 /* call first */, HandleXactAccessViolation);
    if (!g_vehHandle) {
        ProxyLog("InstallXactGuards: AddVectoredExceptionHandler FAILED (GetLastError=%lu)",
                 (unsigned long)GetLastError());
    }

    MH_STATUS initStatus = MH_Initialize();
    if (initStatus != MH_OK && initStatus != MH_ERROR_ALREADY_INITIALIZED) {
        ProxyLog("InstallXactGuards: MH_Initialize failed: %d", (int)initStatus);
        return;
    }

    InstallOneHook("+0x1D751", (BYTE *)g_xactBase + 0x1D751, (void *)TeardownDetour_0x1D751,
                   (void **)&s_realTeardownFn, g_flags.hook1D751);
    InstallOneHook("+0x1D212", (BYTE *)g_xactBase + 0x1D212, (void *)NullHopDetour_0x1D212,
                   (void **)&s_realNullHopFn, g_flags.hook1D212);
    InstallOneHook("+0x1DE46", (BYTE *)g_xactBase + 0x1DE46, (void *)ListWalkDetour_0x1DE46,
                   (void **)&s_realListWalkFn, g_flags.hook1DE46);

    ProxyLog("InstallXactGuards: setup complete");
}

/* ---- shared watcher-thread entry point, usable by any loader ---- */

static unsigned __stdcall WatcherThread(void *param)
{
    (void)param;

    ProxyLog("WatcherThread: started, polling for xactengine2_9.dll");

    HMODULE xactBase = NULL;
    for (int i = 0; i < 300; i++) { /* ~300 * 200ms = 60s max wait */
        xactBase = GetModuleHandleW(L"xactengine2_9.dll");
        if (xactBase) break;
        Sleep(200);
    }

    if (xactBase) {
        ProxyLog("WatcherThread: xactengine2_9.dll found at %p after polling, installing guards", xactBase);
        InstallXactGuards(xactBase);
    } else {
        ProxyLog("WatcherThread: xactengine2_9.dll never appeared after 60s - guards NOT installed "
                 "(game may not use XACT this session, or loaded it unusually slowly)");
    }

    return 0;
}

void StartXactGuardsWatcher(HMODULE selfModule)
{
    ProxyLogInit(selfModule); /* must run before any ProxyLog/ProxyGetDir call, anywhere */
    ProxyLog("StartXactGuardsWatcher: spawning watcher thread");
    uintptr_t h = _beginthreadex(NULL, 0, WatcherThread, NULL, 0, NULL);
    if (h) CloseHandle((HANDLE)h);
}
