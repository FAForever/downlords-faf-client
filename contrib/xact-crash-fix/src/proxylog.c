/*
 * proxylog.c - path resolution + the composition root for logging.
 *
 * The actual logging behavior (which sinks are active, what the minimum
 * level is) now lives behind a real interface (ILogSink, see ilog_sink.h)
 * with dependency injection: CreateDebugStringSink()/CreateFileSink()
 * build sink objects, and ProxyLogConfigure() injects whichever ones are
 * wanted into a Logger (logger.h) - the sinks and the Logger have no idea
 * OutputDebugStringA or fopen() exist at the call-site level, they just
 * implement/consume ILogSink.
 *
 * This ISN'T textbook parameter-passing DI, though, and that's deliberate:
 * several call sites (xact_guards.c's MinHook detour functions) have
 * signatures fixed by the game binary's own calling convention - a
 * `Logger *` can't be threaded through them as an explicit parameter
 * without breaking the ABI MinHook's trampoline relies on. So injection
 * happens at ONE composition root instead (right here, in
 * ProxyLogConfigure, called once after config is parsed) - the resulting
 * Logger* is stored in a module-level static, and the free-function API
 * below (LogInfo() etc.) looks it up internally. The actual behavior
 * swap (which sinks, what level) still happens entirely at the
 * composition root, not hardcoded into the logging calls - that's the
 * part that matters for testability/swappability, even though the
 * *lookup* is global rather than passed-in.
 *
 * ProxyGetDir() is also used by flags.c, which needs to find
 * xact_proxy_flags.ini next to wherever this code is actually loaded
 * from - portable across machines, not a hardcoded path.
 */
#include <windows.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include "proxylog.h"
#include "logger.h"
#include "debug_string_sink.h"
#include "file_sink.h"

#define LOG_FLAG L"/logxactguards"
#define DEFAULT_LOG_FILENAME "xact_proxy_runtime.log"

static char g_moduleDirectory[MAX_PATH] = "";  /* directory this module loaded from, trailing backslash */
static BOOL g_pathsInitialized = FALSE;
static BOOL g_cmdLineForceFile = FALSE;

/* The composition root's output. Never NULL after this file's own static
 * initialization below - CreateDefaultLogger() runs at first use if
 * ProxyLogConfigure() hasn't been called yet, so early startup messages
 * (before config is parsed) still go somewhere sensible instead of
 * vanishing. */
static Logger *g_activeLogger = NULL;

static Logger *CreateDefaultLogger(void)
{
    Logger *logger = CreateLogger();
    if (!logger) return NULL;
    LoggerSetMinLevel(logger, LOG_INFO);
    LoggerAddSink(logger, CreateDebugStringSink()); /* file sink stays off until config says otherwise */
    return logger;
}

static Logger *GetActiveLogger(void)
{
    if (!g_activeLogger) {
        g_activeLogger = CreateDefaultLogger();
    }
    return g_activeLogger;
}

/* Checks THIS PROCESS's own command line (not an arbitrary string - always
 * GetCommandLineW()) for a flag, case-insensitively. Manual search to avoid
 * pulling in shlwapi.dll just for StrStrIW. */
static BOOL CommandLineHasFlag(const wchar_t *flag)
{
    size_t flagLen = wcslen(flag);
    for (const wchar_t *p = GetCommandLineW(); *p; p++) {
        if (_wcsnicmp(p, flag, flagLen) == 0) {
            return TRUE;
        }
    }
    return FALSE;
}

void ProxyLogInit(HMODULE selfModule)
{
    char dllPath[MAX_PATH];
    if (GetModuleFileNameA(selfModule, dllPath, MAX_PATH) == 0) {
        return; /* g_pathsInitialized stays FALSE - ProxyGetDir becomes a no-op rather than guessing a path */
    }

    char *lastSlash = strrchr(dllPath, '\\');
    if (!lastSlash) {
        return;
    }

    size_t dirLen = (size_t)(lastSlash - dllPath) + 1; /* include the slash */
    if (dirLen >= MAX_PATH) {
        return;
    }
    memcpy(g_moduleDirectory, dllPath, dirLen);
    g_moduleDirectory[dirLen] = '\0';

    g_pathsInitialized = TRUE;
    g_cmdLineForceFile = CommandLineHasFlag(LOG_FLAG);
}

const char *ProxyGetDir(void)
{
    return g_pathsInitialized ? g_moduleDirectory : "";
}

void ProxyLogConfigure(LogLevel minLevel, BOOL debugStringEnabled, const char *fileNameOrNull)
{
    /* Composition root: replace whatever logger was active (the default
     * one, if this is the first real configure call) with a freshly wired
     * one reflecting the requested sinks/level. */
    Logger *oldLogger = g_activeLogger;

    Logger *logger = CreateLogger();
    if (!logger) return; /* leave g_activeLogger as it was rather than dropping logging entirely */

    LoggerSetMinLevel(logger, minLevel);

    if (debugStringEnabled) {
        LoggerAddSink(logger, CreateDebugStringSink());
    }

    const char *effectiveFileName = fileNameOrNull;
    if ((!effectiveFileName || effectiveFileName[0] == '\0') && g_cmdLineForceFile) {
        effectiveFileName = DEFAULT_LOG_FILENAME; /* one-launch override, config left the file sink off */
    }

    if (g_pathsInitialized && effectiveFileName && effectiveFileName[0] != '\0' &&
        strlen(g_moduleDirectory) + strlen(effectiveFileName) < MAX_PATH) {
        char fullPath[MAX_PATH];
        strcpy(fullPath, g_moduleDirectory);
        strcat(fullPath, effectiveFileName);
        LoggerAddSink(logger, CreateFileSink(fullPath));
    }

    g_activeLogger = logger;
    if (oldLogger) {
        LoggerDestroy(oldLogger);
    }
}

LogLevel ProxyLogLevelFromString(const char *s)
{
    if (!s) return LOG_INFO;
    if (_stricmp(s, "trace") == 0)                              return LOG_TRACE;
    if (_stricmp(s, "debug") == 0)                              return LOG_DEBUG;
    if (_stricmp(s, "info") == 0)                               return LOG_INFO;
    if (_stricmp(s, "warn") == 0 || _stricmp(s, "warning") == 0) return LOG_WARN;
    if (_stricmp(s, "error") == 0)                              return LOG_ERROR;
    if (_stricmp(s, "none") == 0 || _stricmp(s, "off") == 0)    return LOG_NONE;
    return LOG_INFO; /* unrecognized -> sensible default, not silently "off" */
}

void ProxyLogAt(LogLevel level, const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    LoggerLogV(GetActiveLogger(), level, fmt, ap);
    va_end(ap);
}

void LogTrace(const char *fmt, ...) { va_list ap; va_start(ap, fmt); LoggerLogV(GetActiveLogger(), LOG_TRACE, fmt, ap); va_end(ap); }
void LogDebug(const char *fmt, ...) { va_list ap; va_start(ap, fmt); LoggerLogV(GetActiveLogger(), LOG_DEBUG, fmt, ap); va_end(ap); }
void LogInfo (const char *fmt, ...) { va_list ap; va_start(ap, fmt); LoggerLogV(GetActiveLogger(), LOG_INFO,  fmt, ap); va_end(ap); }
void LogWarn (const char *fmt, ...) { va_list ap; va_start(ap, fmt); LoggerLogV(GetActiveLogger(), LOG_WARN,  fmt, ap); va_end(ap); }
void LogError(const char *fmt, ...) { va_list ap; va_start(ap, fmt); LoggerLogV(GetActiveLogger(), LOG_ERROR, fmt, ap); va_end(ap); }

void ProxyLog(const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    LoggerLogV(GetActiveLogger(), LOG_INFO, fmt, ap);
    va_end(ap);
}
