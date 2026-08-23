/*
 * proxylog.c - leveled diagnostic logging for the xactengine2_9.dll
 * crash-containment guards, with two independently-configurable sinks:
 * OutputDebugStringA (picked up natively by any attached debugger -
 * FADeepProbe, WinDbg, DebugView) and an optional file next to wherever
 * this DLL is actually loaded from. Both the minimum level and which
 * sinks are active are set via ProxyLogConfigure(), driven by
 * xact_proxy_flags.ini (see flags.c) - so logging behavior is entirely
 * config-driven, no rebuild needed to change it.
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

#define LOG_FLAG L"/logxactguards"
#define DEFAULT_LOG_FILENAME "xact_proxy_runtime.log"

static char g_moduleDirectory[MAX_PATH] = "";  /* directory this module loaded from, trailing backslash */
static char g_logFilePath[MAX_PATH] = "";
static BOOL g_pathsInitialized = FALSE;
static BOOL g_cmdLineForceFile = FALSE;

static LogLevel g_minLevel = LOG_INFO;
static BOOL g_debugStringEnabled = TRUE;
static BOOL g_fileLoggingEnabled = FALSE;

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
    g_minLevel = minLevel;
    g_debugStringEnabled = debugStringEnabled;

    const char *effectiveFileName = fileNameOrNull;
    if ((!effectiveFileName || effectiveFileName[0] == '\0') && g_cmdLineForceFile) {
        effectiveFileName = DEFAULT_LOG_FILENAME; /* one-launch override, config left it off */
    }

    if (g_pathsInitialized && effectiveFileName && effectiveFileName[0] != '\0' &&
        strlen(g_moduleDirectory) + strlen(effectiveFileName) < MAX_PATH) {
        strcpy(g_logFilePath, g_moduleDirectory);
        strcat(g_logFilePath, effectiveFileName);
        g_fileLoggingEnabled = TRUE;
    } else {
        g_fileLoggingEnabled = FALSE;
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

static const char *LevelTag(LogLevel level)
{
    switch (level) {
        case LOG_TRACE: return "TRACE";
        case LOG_DEBUG: return "DEBUG";
        case LOG_INFO:  return "INFO";
        case LOG_WARN:  return "WARN";
        case LOG_ERROR: return "ERROR";
        default:        return "?";
    }
}

static void ProxyLogAtV(LogLevel level, const char *fmt, va_list ap)
{
    if (level < g_minLevel) return;
    if (!g_debugStringEnabled && !g_fileLoggingEnabled) return;

    char msg[1024];
    int n = _snprintf(msg, sizeof(msg) - 2, "[xact_guards] [%s] [tid=%lu] ",
                       LevelTag(level), (unsigned long)GetCurrentThreadId());
    if (n < 0) n = 0;
    if ((size_t)n >= sizeof(msg) - 2) n = (int)sizeof(msg) - 2;

    int n2 = _vsnprintf(msg + n, sizeof(msg) - (size_t)n - 2, fmt, ap);
    if (n2 < 0) n2 = 0;

    size_t total = (size_t)n + (size_t)n2;
    if (total > sizeof(msg) - 2) total = sizeof(msg) - 2;
    msg[total] = '\n';
    msg[total + 1] = '\0';

    if (g_debugStringEnabled) {
        OutputDebugStringA(msg);
    }

    if (g_fileLoggingEnabled) {
        FILE *f = fopen(g_logFilePath, "a");
        if (f) {
            SYSTEMTIME st;
            GetLocalTime(&st);
            fprintf(f, "[%04d-%02d-%02d %02d:%02d:%02d.%03d] %s",
                    st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond, st.wMilliseconds, msg);
            fclose(f);
        }
    }
}

void ProxyLogAt(LogLevel level, const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    ProxyLogAtV(level, fmt, ap);
    va_end(ap);
}

void LogTrace(const char *fmt, ...) { va_list ap; va_start(ap, fmt); ProxyLogAtV(LOG_TRACE, fmt, ap); va_end(ap); }
void LogDebug(const char *fmt, ...) { va_list ap; va_start(ap, fmt); ProxyLogAtV(LOG_DEBUG, fmt, ap); va_end(ap); }
void LogInfo (const char *fmt, ...) { va_list ap; va_start(ap, fmt); ProxyLogAtV(LOG_INFO,  fmt, ap); va_end(ap); }
void LogWarn (const char *fmt, ...) { va_list ap; va_start(ap, fmt); ProxyLogAtV(LOG_WARN,  fmt, ap); va_end(ap); }
void LogError(const char *fmt, ...) { va_list ap; va_start(ap, fmt); ProxyLogAtV(LOG_ERROR, fmt, ap); va_end(ap); }

void ProxyLog(const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    ProxyLogAtV(LOG_INFO, fmt, ap);
    va_end(ap);
}
