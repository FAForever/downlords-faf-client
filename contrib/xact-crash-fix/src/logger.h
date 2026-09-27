#ifndef LOGGER_H
#define LOGGER_H

#include <stdarg.h>
#include "ilog_sink.h"

/* Opaque - the dependency-injection container for log sinks. Formats each
 * message once, then dispatches to every registered sink (if the message
 * clears the logger's single minimum level - level-gating happens here,
 * not per-sink, matching how this behaved before this was split out). */
typedef struct Logger Logger;

Logger *CreateLogger(void);

/* Logger takes ownership - calls sink->Destroy on it when LoggerDestroy
 * runs. A NULL sink is silently ignored (lets callers do
 * LoggerAddSink(logger, CreateFileSink(path)) without a NULL check when
 * CreateFileSink() might reasonably return NULL for "no file wanted"). */
void LoggerAddSink(Logger *logger, ILogSink *sink);

void LoggerSetMinLevel(Logger *logger, LogLevel level);

void LoggerLog(Logger *logger, LogLevel level, const char *fmt, ...);
void LoggerLogV(Logger *logger, LogLevel level, const char *fmt, va_list args);

/* Detaches and destroys every sink, then frees the logger itself. */
void LoggerDestroy(Logger *logger);

#endif
