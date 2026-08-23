#include <windows.h>
#include <stdio.h>
#include <string.h>
#include "flags.h"
#include "proxylog.h"

/* Defaults: every guard fully active - editing/deleting the config file
 * never reduces protection below "everything on". */
XactFlags g_flags = { STRAT_CONTAIN, STRAT_CONTAIN, STRAT_CONTAIN };

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

static void WriteTemplateFile(const char *path)
{
    FILE *f = fopen(path, "w");
    if (!f) return;
    fprintf(f,
        "# xact_proxy feature flags - edit this file and relaunch the game to try\n"
        "# different things. Re-read once at DLL load, no rebuild needed.\n"
        "#\n"
        "# Values: disabled | passthrough | contain\n"
        "#   disabled    - hook not installed at all (same as no wrapper for this site)\n"
        "#   passthrough - hook installed, but just calls the original function with no\n"
        "#                 recovery - a crash here still crashes, but the VEH still logs it\n"
        "#                 (useful to check whether a hook itself changes anything)\n"
        "#   contain     - full recovery: catch the crash, log it, keep the game running\n"
        "#\n"
        "HOOK_1D751=contain\n"
        "HOOK_1D212=contain\n"
        "HOOK_1DE46=contain\n"
    );
    fclose(f);
}

void LoadXactFlags(void)
{
    char flagsPath[MAX_PATH];
    GetFlagsPath(flagsPath, sizeof(flagsPath));

    FILE *f = fopen(flagsPath, "r");
    if (!f) {
        ProxyLog("LoadXactFlags: no config file at \"%s\" - using defaults (all contain), "
                 "writing a template for next time", flagsPath);
        WriteTemplateFile(flagsPath);
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
    }
    fclose(f);

    ProxyLog("LoadXactFlags: loaded from \"%s\" -> hook1D751=%s hook1D212=%s hook1DE46=%s",
             flagsPath, StrategyName(g_flags.hook1D751), StrategyName(g_flags.hook1D212),
             StrategyName(g_flags.hook1DE46));
}
