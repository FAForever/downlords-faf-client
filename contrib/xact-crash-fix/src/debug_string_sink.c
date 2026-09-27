#include <windows.h>
#include <stdlib.h>
#include "debug_string_sink.h"

static void DebugStringSink_Write(ILogSink *self, LogLevel level, const char *formattedMessage)
{
    (void)self;
    (void)level;
    OutputDebugStringA(formattedMessage);
}

static void DebugStringSink_Destroy(ILogSink *self)
{
    free(self);
}

ILogSink *CreateDebugStringSink(void)
{
    ILogSink *sink = (ILogSink *)malloc(sizeof(ILogSink));
    if (!sink) return NULL;
    sink->Write = DebugStringSink_Write;
    sink->Destroy = DebugStringSink_Destroy;
    return sink;
}
