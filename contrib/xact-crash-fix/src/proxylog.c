/*
 * proxylog.c - diagnostic output for the xactengine2_9.dll crash-containment
 * guards. Always goes to OutputDebugStringA (any attached debugger - FADeepProbe,
 * WinDbg, DebugView - picks these up natively as OUTPUT_DEBUG_STRING events).
 * ADDITIONALLY writes to a log file if "/logxactguards" appears anywhere on
 * this process's own command line (same convention the game itself uses for
 * its own flags - /init, /log, /nobugreport, etc.) - lets you opt into a
 * persistent, after-the-fact-reviewable log per launch without editing any
 * config file or needing DebugView running.
 *
 * ProxyGetDir() is kept for flags.c, which needs to find xact_proxy_flags.ini
 * next to wherever this code is actually loaded from - portable across
 * machines, not a hardcoded path.
 */
#include <windows.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include "proxylog.h"

#define LOG_FLAG L"/logxactguards"

static char g_dir[MAX_PATH] = "";      /* directory this module loaded from, trailing backslash */
static char g_logPath[MAX_PATH] = "";
static BOOL g_ready = FALSE;
static BOOL g_fileLoggingEnabled = FALSE;

/* Manual case-insensitive wide substring search - avoids pulling in
 * shlwapi.dll just for StrStrIW. */
static BOOL ContainsFlagCI(const wchar_t *haystack, const wchar_t *needle)
{
    size_t needleLen = wcslen(needle);
    for (const wchar_t *p = haystack; *p; p++) {
        if (_wcsnicmp(p, needle, needleLen) == 0) {
            return TRUE;
        }
    }
    return FALSE;
}

void ProxyLogInit(HMODULE selfModule)
{
    char dllPath[MAX_PATH];
    if (GetModuleFileNameA(selfModule, dllPath, MAX_PATH) == 0) {
        return; /* g_ready stays FALSE - ProxyGetDir becomes a no-op rather than guessing a path */
    }

    char *lastSlash = strrchr(dllPath, '\\');
    if (!lastSlash) {
        return;
    }

    size_t dirLen = (size_t)(lastSlash - dllPath) + 1; /* include the slash */
    if (dirLen >= MAX_PATH) {
        return;
    }
    memcpy(g_dir, dllPath, dirLen);
    g_dir[dirLen] = '\0';

    g_ready = TRUE;

    g_fileLoggingEnabled = ContainsFlagCI(GetCommandLineW(), LOG_FLAG);
    if (g_fileLoggingEnabled) {
        if (dirLen + strlen("xact_proxy_runtime.log") < MAX_PATH) {
            strcpy(g_logPath, g_dir);
            strcat(g_logPath, "xact_proxy_runtime.log");
        } else {
            g_fileLoggingEnabled = FALSE; /* path too long, fall back to debug-string only */
        }
    }
}

const char *ProxyGetDir(void)
{
    return g_ready ? g_dir : "";
}

void ProxyLog(const char *fmt, ...)
{
    char msg[1024];
    int n = _snprintf(msg, sizeof(msg) - 2, "[xact_guards] [tid=%lu] ", (unsigned long)GetCurrentThreadId());
    if (n < 0) n = 0;
    if ((size_t)n >= sizeof(msg) - 2) n = (int)sizeof(msg) - 2;

    va_list ap;
    va_start(ap, fmt);
    int n2 = _vsnprintf(msg + n, sizeof(msg) - (size_t)n - 2, fmt, ap);
    va_end(ap);
    if (n2 < 0) n2 = 0;

    size_t total = (size_t)n + (size_t)n2;
    if (total > sizeof(msg) - 2) total = sizeof(msg) - 2;
    msg[total] = '\n';
    msg[total + 1] = '\0';

    OutputDebugStringA(msg);

    if (g_fileLoggingEnabled) {
        FILE *f = fopen(g_logPath, "a");
        if (f) {
            SYSTEMTIME st;
            GetLocalTime(&st);
            fprintf(f, "[%04d-%02d-%02d %02d:%02d:%02d.%03d] %s",
                    st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond, st.wMilliseconds, msg);
            fclose(f);
        }
    }
}
