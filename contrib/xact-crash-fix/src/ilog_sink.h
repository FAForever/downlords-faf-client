#ifndef ILOG_SINK_H
#define ILOG_SINK_H

#include "proxylog.h" /* LogLevel */

/* A log sink is anything that can receive a fully-formatted log line - a
 * debugger (OutputDebugStringA), a file, or a test double. Plain C, no
 * inheritance: a struct of function pointers is the interface, and each
 * concrete sink is just a struct that starts with one of these (or embeds
 * it as first member) plus whatever private state it needs after that -
 * the classic C "vtable" idiom.
 *
 * self->Write's formattedMessage already has the "[xact_guards] [LEVEL]
 * [tid=...]" prefix and trailing newline applied by Logger - sinks just
 * write it as-is, they don't reformat. */
typedef struct ILogSink {
    void (*Write)(struct ILogSink *self, LogLevel level, const char *formattedMessage);
    void (*Destroy)(struct ILogSink *self); /* frees self and any owned resources; NULL if nothing to free */
} ILogSink;

#endif
