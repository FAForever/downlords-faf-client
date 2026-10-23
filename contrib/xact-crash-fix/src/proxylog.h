#ifndef PROXYLOG_H
#define PROXYLOG_H
#include <windows.h>

/* Matches the FAF project's own logging convention (TRACE < DEBUG < INFO <
 * WARN < ERROR). LOG_NONE is a sentinel for "suppress everything", usable
 * as a configured minimum level, not as a level to log AT. */
typedef enum {
    LOG_TRACE = 0,
    LOG_DEBUG = 1,
    LOG_INFO  = 2,
    LOG_WARN  = 3,
    LOG_ERROR = 4,
    LOG_NONE  = 5
} LogLevel;

/* Call once from DllMain, before any other thread might use any of the
 * functions below - not thread-safe by itself, relies on DllMain's own
 * loader-lock-serialized guarantee of running exactly once, first. */
void ProxyLogInit(HMODULE selfModule);

/* Directory this DLL itself was loaded from (trailing backslash included),
 * e.g. "C:\ProgramData\FAForever\bin\" - portable across machines, unlike
 * a hardcoded path. Other modules (flags.c) build their own filenames off
 * this instead of hardcoding a path too. Returns "" if ProxyLogInit
 * hasn't run yet or failed. */
const char *ProxyGetDir(void);

/* Sets the minimum level that gets emitted, whether OutputDebugStringA is
 * used, and whether/where a file sink writes - call after parsing config
 * (flags.c). Before this runs, sensible defaults apply on their own
 * (INFO, debug-string on, file off), so early log calls (including
 * LoadXactFlags' own "config loaded from..." message) still behave
 * reasonably.
 *
 * fileNameOrNull is a bare filename (e.g. "xact_proxy_runtime.log"),
 * resolved relative to ProxyGetDir() internally - never a full path, so
 * the portability guarantee above holds regardless of what's configured.
 * NULL or "" disables the file sink - UNLESS "/logxactguards" is present
 * on this process's own command line, which forces the file sink on with
 * a default filename regardless of config, as a quick one-launch override
 * for whoever can't easily edit the config file first. */
void ProxyLogConfigure(LogLevel minLevel, BOOL debugStringEnabled, const char *fileNameOrNull);

/* Case-insensitive "trace"/"debug"/"info"/"warn"/"warning"/"error"/"none"/"off";
 * anything else (including NULL) returns LOG_INFO. */
LogLevel ProxyLogLevelFromString(const char *s);

void ProxyLogAt(LogLevel level, const char *fmt, ...);
void LogTrace(const char *fmt, ...);
void LogDebug(const char *fmt, ...);
void LogInfo(const char *fmt, ...);
void LogWarn(const char *fmt, ...);
void LogError(const char *fmt, ...);

/* Back-compat shim (logs at LOG_INFO) for call sites not yet migrated to
 * the leveled API above (xact_guards.c, dllmain.c, guards_dllmain.c, as
 * of this writing - migrate them and retire this). */
void ProxyLog(const char *fmt, ...);

#endif
