#ifndef DEBUG_STRING_SINK_H
#define DEBUG_STRING_SINK_H

#include "ilog_sink.h"

/* A sink that writes to OutputDebugStringA - picked up natively by any
 * attached debugger (FADeepProbe, WinDbg, DebugView). Stateless, but
 * still heap-allocated so LoggerDestroy can uniformly call Destroy on
 * every sink regardless of type. */
ILogSink *CreateDebugStringSink(void);

#endif
