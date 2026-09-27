#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include "logger.h"

#define MAX_SINKS 8

struct Logger {
    ILogSink *sinks[MAX_SINKS];
    int sinkCount;
    LogLevel minLevel;
};

Logger *CreateLogger(void)
{
    Logger *logger = (Logger *)malloc(sizeof(Logger));
    if (!logger) return NULL;
    logger->sinkCount = 0;
    logger->minLevel = LOG_INFO;
    return logger;
}

void LoggerAddSink(Logger *logger, ILogSink *sink)
{
    if (!logger || !sink) return;
    if (logger->sinkCount >= MAX_SINKS) return; /* fixed-size on purpose - we only ever have 2 sink types today */
    logger->sinks[logger->sinkCount++] = sink;
}

void LoggerSetMinLevel(Logger *logger, LogLevel level)
{
    if (!logger) return;
    logger->minLevel = level;
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

void LoggerLogV(Logger *logger, LogLevel level, const char *fmt, va_list args)
{
    if (!logger) return;
    if (level < logger->minLevel) return;
    if (logger->sinkCount == 0) return;

    char msg[1024];
    int n = _snprintf(msg, sizeof(msg) - 2, "[xact_guards] [%s] [tid=%lu] ",
                       LevelTag(level), (unsigned long)GetCurrentThreadId());
    if (n < 0) n = 0;
    if ((size_t)n >= sizeof(msg) - 2) n = (int)sizeof(msg) - 2;

    int n2 = _vsnprintf(msg + n, sizeof(msg) - (size_t)n - 2, fmt, args);
    if (n2 < 0) n2 = 0;

    size_t total = (size_t)n + (size_t)n2;
    if (total > sizeof(msg) - 2) total = sizeof(msg) - 2;
    msg[total] = '\n';
    msg[total + 1] = '\0';

    for (int i = 0; i < logger->sinkCount; i++) {
        logger->sinks[i]->Write(logger->sinks[i], level, msg);
    }
}

void LoggerLog(Logger *logger, LogLevel level, const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    LoggerLogV(logger, level, fmt, args);
    va_end(args);
}

void LoggerDestroy(Logger *logger)
{
    if (!logger) return;
    for (int i = 0; i < logger->sinkCount; i++) {
        if (logger->sinks[i]->Destroy) {
            logger->sinks[i]->Destroy(logger->sinks[i]);
        }
    }
    free(logger);
}
