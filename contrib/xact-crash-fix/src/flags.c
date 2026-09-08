#include <windows.h>
#include <stdio.h>
#include <string.h>
#include "flags.h"
#include "proxylog.h"

/* Defaults: every guard fully active - editing/deleting the config file
 * never reduces protection below "everything on". Logging defaults to
 * INFO + debug-string on + file off (ProxyLogConfigure gets called with
 * these below even when no config file exists, so this struct literal
 * is only the in-memory fallback until that call runs). */
XactFlags g_flags = { STRAT_CONTAIN, STRAT_CONTAIN, STRAT_CONTAIN, LOG_INFO, TRUE, "" };

/* Builds "<dir this DLL loaded from>\xact_proxy_flags.ini" - portable,
 * no hardcoded path (see proxylog.c's ProxyGetDir for why). */
static void GetFlagsPath(char *out, size_t outSize)
{
    const char *dir = ProxyGetDir();
    if (dir[0] == '\0') {
        /* ProxyLogInit hasn't run / failed - fall back to relative path
         * (current directory), same spirit as proxylog.c's own fallback */
        strncpy(out, "xact_proxy_flags.ini", outSize - 1);
        out[outSize - 1] = '\0';
        return;
    }
    snprintf(out, outSize, "%sxact_proxy_flags.ini", dir);
}

static GuardStrategy ParseStrategy(const char *v)
{
    if (_stricmp(v, "disabled") == 0)    return STRAT_DISABLED;
    if (_stricmp(v, "passthrough") == 0) return STRAT_PASSTHROUGH;
    if (_stricmp(v, "contain") == 0)     return STRAT_CONTAIN;
    return STRAT_CONTAIN; /* unrecognized value -> safest default, not "off" */
}

static const char *StrategyName(GuardStrategy s)
{
    switch (s) {
        case STRAT_DISABLED:    return "disabled";
        case STRAT_PASSTHROUGH: return "passthrough";
        case STRAT_CONTAIN:     return "contain";
        default:                return "?";
    }
}

static BOOL ParseBool(const char *v, BOOL defaultValue)
{
    if (_stricmp(v, "true") == 0 || _stricmp(v, "1") == 0 || _stricmp(v, "yes") == 0)  return TRUE;
    if (_stricmp(v, "false") == 0 || _stricmp(v, "0") == 0 || _stricmp(v, "no") == 0)  return FALSE;
    return defaultValue; /* unrecognized -> keep current default rather than guess */
}

static void WriteTemplateFile(const char *path)
{
    FILE *f = fopen(path, "w");
    if (!f) return;
    fprintf(f,
        "# xact_proxy feature flags - edit this file and relaunch the game to try\n"
        "# different things. Re-read once at DLL load, no rebuild needed.\n"
        "#\n"
        "# Guard strategy values: disabled | passthrough | contain\n"
        "#   disabled    - hook not installed at all (same as no wrapper for this site)\n"
        "#   passthrough - hook installed, but just calls the original function with no\n"
        "#                 recovery - a crash here still crashes, but the VEH still logs it\n"
        "#                 (useful to check whether a hook itself changes anything)\n"
        "#   contain     - full recovery: catch the crash, log it, keep the game running\n"
        "#\n"
        "HOOK_1D751=contain\n"
        "HOOK_1D212=contain\n"
        "HOOK_1DE46=contain\n"
        "#\n"
        "# Logging - matches FAF's own TRACE/DEBUG/INFO/WARN/ERROR convention.\n"
        "# LOG_LEVEL: minimum level that gets emitted (messages below this are dropped).\n"
        "# LOG_DEBUGSTRING: true/false - send output to OutputDebugStringA (any attached\n"
        "#   debugger - FADeepProbe, WinDbg, DebugView - picks this up natively).\n"
        "# LOG_FILE: bare filename (e.g. xact_proxy_runtime.log), resolved next to this\n"
        "#   DLL - leave empty to disable the file sink. Passing /logxactguards on the\n"
        "#   game's own command line forces the file sink on for that one launch\n"
        "#   regardless of this setting, as a quick override.\n"
        "#\n"
        "LOG_LEVEL=info\n"
        "LOG_DEBUGSTRING=true\n"
        "LOG_FILE=\n"
    );
    fclose(f);
}

void LoadXactFlags(void)
{
    char flagsPath[MAX_PATH];
    GetFlagsPath(flagsPath, sizeof(flagsPath));

    FILE *f = fopen(flagsPath, "r");
    if (!f) {
        ProxyLog("LoadXactFlags: no config file at \"%s\" - using defaults (all contain, "
                 "log level info), writing a template for next time", flagsPath);
        WriteTemplateFile(flagsPath);
        ProxyLogConfigure(g_flags.logLevel, g_flags.logDebugStringEnabled, g_flags.logFileName);
        return;
    }

    char line[256];
    while (fgets(line, sizeof(line), f)) {
        char *nl = strpbrk(line, "\r\n");
        if (nl) *nl = 0;
        if (line[0] == '#' || line[0] == '\0') continue;

        char *eq = strchr(line, '=');
        if (!eq) continue;
        *eq = '\0';
        const char *key = line;
        const char *val = eq + 1;

        if (_stricmp(key, "HOOK_1D751") == 0)      g_flags.hook1D751 = ParseStrategy(val);
        else if (_stricmp(key, "HOOK_1D212") == 0) g_flags.hook1D212 = ParseStrategy(val);
        else if (_stricmp(key, "HOOK_1DE46") == 0) g_flags.hook1DE46 = ParseStrategy(val);
        else if (_stricmp(key, "LOG_LEVEL") == 0)  g_flags.logLevel = ProxyLogLevelFromString(val);
        else if (_stricmp(key, "LOG_DEBUGSTRING") == 0)
            g_flags.logDebugStringEnabled = ParseBool(val, g_flags.logDebugStringEnabled);
        else if (_stricmp(key, "LOG_FILE") == 0)
            strncpy(g_flags.logFileName, val, sizeof(g_flags.logFileName) - 1);
    }
    fclose(f);

    /* Apply the logging settings before the summary line below, so that
     * line itself already reflects the newly-configured level/sinks. */
    ProxyLogConfigure(g_flags.logLevel, g_flags.logDebugStringEnabled, g_flags.logFileName);

    ProxyLog("LoadXactFlags: loaded from \"%s\" -> hook1D751=%s hook1D212=%s hook1DE46=%s "
             "logDebugString=%s logFile=\"%s\"",
             flagsPath, StrategyName(g_flags.hook1D751), StrategyName(g_flags.hook1D212),
             StrategyName(g_flags.hook1DE46),
             g_flags.logDebugStringEnabled ? "true" : "false", g_flags.logFileName);
}
