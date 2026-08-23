#ifndef XACT_FLAGS_H
#define XACT_FLAGS_H

/* Strategy pattern: each guarded site's runtime behavior is one of three
 * interchangeable strategies, selected via an external config file (no
 * rebuild needed to switch) - read once per game launch. */
typedef enum {
    STRAT_DISABLED    = 0, /* hook not installed at all - identical to pre-wrapper behavior for this site */
    STRAT_PASSTHROUGH = 1, /* hook installed, but detour just calls the original with no try/catch -
                             * useful for isolating "does the hook itself change anything" from
                             * "does the recovery logic work", since a crash here still crashes
                             * the process same as if unhooked, but still gets logged by the VEH */
    STRAT_CONTAIN     = 2  /* full setjmp/longjmp recovery - the original wrapper behavior */
} GuardStrategy;

typedef struct {
    GuardStrategy hook1D751;  /* use-after-free / dangling-pointer teardown site (§2.7) */
    GuardStrategy hook1D212;  /* null pointer-chain-hop site (§2.7) */
    GuardStrategy hook1DE46;  /* linked-list iterator-invalidation site (§2.7c) */
} XactFlags;

extern XactFlags g_flags;

/* Reads C:\Users\admin\Desktop\supreme commander crash debug\xact_proxy_flags.ini
 * if present; any missing/unrecognized key keeps its default (STRAT_CONTAIN -
 * i.e. full protection - so a missing or malformed config file never silently
 * turns protection off). Writes out a template file if none exists yet. */
void LoadXactFlags(void);

#endif
