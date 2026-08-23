#ifndef PROXYLOG_H
#define PROXYLOG_H
#include <windows.h>

/* Call once from DllMain, before any other thread might use ProxyLog or
 * ProxyGetDir - not thread-safe by itself, relies on DllMain's own
 * loader-lock-serialized guarantee of running exactly once, first. */
void ProxyLogInit(HMODULE selfModule);

/* Directory this DLL itself was loaded from (trailing backslash included),
 * e.g. "C:\ProgramData\FAForever\bin\" - portable across machines, unlike
 * a hardcoded path. Other modules (flags.c) build their own filenames off
 * this instead of hardcoding a path too. Returns "" if ProxyLogInit
 * hasn't run yet or failed. */
const char *ProxyGetDir(void);

void ProxyLog(const char *fmt, ...);

#endif
