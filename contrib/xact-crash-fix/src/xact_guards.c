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

static __thread jmp_buf   g_recoveryStack[MAX_GUARD_DEPTH];
static __thread const char *g_guardSiteStack[MAX_GUARD_DEPTH];
static __thread int       g_guardDepth = 0;

static void  *g_xactBase = NULL;
static SIZE_T g_xactSize = 0;
static PVOID  g_vehHandle = NULL;

/* Shared "enter a guarded call" helper - every detour follows the exact
 * same shape, this just centralizes the depth/stack bookkeeping so the
 * three detours below only differ in the actual call + its argument list
 * (which C's lack of generics/variadic-safe function pointers means can't
 * be fully collapsed into one function). Returns 1 if the caller should
 * take the "call original directly, no protection" path (either because
 * of PASSTHROUGH strategy or the depth safety valve), 0 if it already
 * handled everything (CONTAIN path, including logging on recovery). */

/* ---- +0x1D751 hook (use-after-free / dangling-pointer teardown site) ---- */

typedef void (__thiscall *Fn_0041d751)(void *this_);
static Fn_0041d751 fpOrig_0041d751 = NULL;

static void __thiscall Detour_0041d751(void *this_)
{
    if (g_flags.hook1D751 == STRAT_PASSTHROUGH) {
        fpOrig_0041d751(this_);
        return;
    }

    if (g_guardDepth >= MAX_GUARD_DEPTH) {
        ProxyLog("hook-1d751: guard stack full (depth=%d), calling through UNGUARDED, this=%p",
                 g_guardDepth, this_);
        fpOrig_0041d751(this_);
        return;
    }

    int depth = g_guardDepth++;
    g_guardSiteStack[depth] = "1d751(UAF-teardown)";

    if (setjmp(g_recoveryStack[depth]) == 0) {
        fpOrig_0041d751(this_);
    } else {
        ProxyLog("hook-1d751: CONTAINED an access violation inside the teardown/UAF site "
                 "(this=%p, depth=%d) - one wave-bank cleanup call was skipped, game should continue",
                 this_, depth);
    }

    g_guardDepth--;
}

/* ---- +0x1D212 hook (null pointer-chain hop, dominant repeat crash) ---- */

typedef void (__thiscall *Fn_0041d212)(void *this_, unsigned char param1);
static Fn_0041d212 fpOrig_0041d212 = NULL;

static void __thiscall Detour_0041d212(void *this_, unsigned char param1)
{
    if (g_flags.hook1D212 == STRAT_PASSTHROUGH) {
        fpOrig_0041d212(this_, param1);
        return;
    }

    if (g_guardDepth >= MAX_GUARD_DEPTH) {
        ProxyLog("hook-1d212: guard stack full (depth=%d), calling through UNGUARDED, this=%p param1=%u",
                 g_guardDepth, this_, (unsigned)param1);
        fpOrig_0041d212(this_, param1);
        return;
    }

    int depth = g_guardDepth++;
    g_guardSiteStack[depth] = "1d212(null-hop)";

    if (setjmp(g_recoveryStack[depth]) == 0) {
        fpOrig_0041d212(this_, param1);
    } else {
        ProxyLog("hook-1d212: CONTAINED an access violation on the null-pointer-chain site "
                 "(this=%p, param1=%u, depth=%d) - that sound/cue lookup was skipped, game should continue",
                 this_, (unsigned)param1, depth);
    }

    g_guardDepth--;
}

/* ---- +0x1DE46 hook (linked-list iterator invalidation, §2.7c) ---- */

typedef void (__thiscall *Fn_0041de46)(void *this_);
static Fn_0041de46 fpOrig_0041de46 = NULL;

static void __thiscall Detour_0041de46(void *this_)
{
    if (g_flags.hook1DE46 == STRAT_PASSTHROUGH) {
        fpOrig_0041de46(this_);
        return;
    }

    if (g_guardDepth >= MAX_GUARD_DEPTH) {
        ProxyLog("hook-1de46: guard stack full (depth=%d), calling through UNGUARDED, this=%p",
                 g_guardDepth, this_);
        fpOrig_0041de46(this_);
        return;
    }

    int depth = g_guardDepth++;
    g_guardSiteStack[depth] = "1de46(list-walk)";

    if (setjmp(g_recoveryStack[depth]) == 0) {
        fpOrig_0041de46(this_);
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

    g_guardDepth--;
}

/* ---- process-wide safety net ---- */

static LONG WINAPI XactVeh(EXCEPTION_POINTERS *ep)
{
    if (ep->ExceptionRecord->ExceptionCode != EXCEPTION_ACCESS_VIOLATION) {
        return EXCEPTION_CONTINUE_SEARCH;
    }

    void *faultAddr = (void *)(UINT_PTR)ep->ContextRecord->Eip;
    BOOL inXactModule = g_xactBase &&
        (BYTE *)faultAddr >= (BYTE *)g_xactBase &&
        (BYTE *)faultAddr <  (BYTE *)g_xactBase + g_xactSize;

    if (g_guardDepth > 0) {
        /* We're inside one of our own guarded calls on THIS thread (TLS -
         * naturally scoped per-thread, so a fault on a different thread
         * can never mistakenly recover here even if that thread also has
         * a guard active). Recover at the innermost active guard. */
        int depth = g_guardDepth - 1;
        ProxyLog("VEH: access violation at %p during guarded call [%s] (depth=%d) - recovering via longjmp",
                 faultAddr, g_guardSiteStack[depth], depth);
        longjmp(g_recoveryStack[depth], 1); /* does not return */
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
                 ep->ContextRecord->Eax, ep->ContextRecord->Ecx, ep->ContextRecord->Edx, ep->ContextRecord->Ebx,
                 ep->ContextRecord->Esp, ep->ContextRecord->Ebp, ep->ContextRecord->Esi, ep->ContextRecord->Edi);
    }

    return EXCEPTION_CONTINUE_SEARCH;
}

static void InstallOneHook(const char *name, void *target, void *detour, void **origOut, GuardStrategy strategy)
{
    if (strategy == STRAT_DISABLED) {
        ProxyLog("InstallXactGuards: %s DISABLED by config, not installing hook (target=%p)", name, target);
        return;
    }

    MH_STATUS st = MH_CreateHook(target, detour, origOut);
    ProxyLog("InstallXactGuards: MH_CreateHook %s (target=%p, strategy=%s) -> %d",
             name, target, strategy == STRAT_PASSTHROUGH ? "passthrough" : "contain", (int)st);
    if (st == MH_OK) {
        st = MH_EnableHook(target);
        ProxyLog("InstallXactGuards: MH_EnableHook %s -> %d", name, (int)st);
    }
}

void InstallXactGuards(HMODULE xactBase)
{
    LoadXactFlags();

    MODULEINFO mi;
    if (GetModuleInformation(GetCurrentProcess(), xactBase, &mi, sizeof(mi))) {
        g_xactBase = mi.lpBaseOfDll;
        g_xactSize = mi.SizeOfImage;
    } else {
        /* Fallback: base is what we were handed, size unknown -> treat as
         * a single point rather than a range for the "inXactModule" check. */
        g_xactBase = xactBase;
        g_xactSize = 1;
    }

    ProxyLog("InstallXactGuards: xactengine2_9.dll base=%p size=%lu",
             g_xactBase, (unsigned long)g_xactSize);

    g_vehHandle = AddVectoredExceptionHandler(1 /* call first */, XactVeh);
    if (!g_vehHandle) {
        ProxyLog("InstallXactGuards: AddVectoredExceptionHandler FAILED (GetLastError=%lu)",
                 (unsigned long)GetLastError());
    }

    MH_STATUS st = MH_Initialize();
    if (st != MH_OK && st != MH_ERROR_ALREADY_INITIALIZED) {
        ProxyLog("InstallXactGuards: MH_Initialize failed: %d", (int)st);
        return;
    }

    InstallOneHook("+0x1D751", (BYTE *)g_xactBase + 0x1D751, (void *)Detour_0041d751,
                   (void **)&fpOrig_0041d751, g_flags.hook1D751);
    InstallOneHook("+0x1D212", (BYTE *)g_xactBase + 0x1D212, (void *)Detour_0041d212,
                   (void **)&fpOrig_0041d212, g_flags.hook1D212);
    InstallOneHook("+0x1DE46", (BYTE *)g_xactBase + 0x1DE46, (void *)Detour_0041de46,
                   (void **)&fpOrig_0041de46, g_flags.hook1DE46);

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
