#ifndef FILE_SINK_H
#define FILE_SINK_H

#include "ilog_sink.h"

/* A sink that appends timestamped lines to a file. Opens/writes/closes
 * per call rather than holding a handle open - these guards fire rarely
 * (only on the fault paths being contained, or sparse INFO-level setup
 * messages), so log-write performance isn't a concern, and this avoids
 * any shared-handle/locking complexity or a leaked-open-handle-on-crash
 * problem.
 *
 * fullPath must already be a complete, resolved path (this sink doesn't
 * know about "relative to the DLL's directory" - that's Logger/proxylog.c's
 * job, keeping this sink a dumb, reusable building block).
 *
 * Returns NULL if fullPath is NULL/empty - caller should treat that as
 * "don't add a file sink" rather than a hard error. A path that's merely
 * unwritable at construction time still returns a sink (each Write just
 * silently no-ops if fopen fails - matches the previous implementation's
 * behavior of not crashing on a bad path). */
ILogSink *CreateFileSink(const char *fullPath);

#endif
