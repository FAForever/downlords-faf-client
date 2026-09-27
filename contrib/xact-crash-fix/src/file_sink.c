#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "file_sink.h"

typedef struct {
    ILogSink base; /* must be first member - callers only ever see &self->base */
    char path[MAX_PATH];
} FileSink;

static void FileSink_Write(ILogSink *self, LogLevel level, const char *formattedMessage)
{
    (void)level;
    FileSink *fileSink = (FileSink *)self;

    FILE *f = fopen(fileSink->path, "a");
    if (!f) return; /* unwritable path - silently drop, don't crash the guarded process over a log write */

    SYSTEMTIME st;
    GetLocalTime(&st);
    fprintf(f, "[%04d-%02d-%02d %02d:%02d:%02d.%03d] %s",
            st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond, st.wMilliseconds,
            formattedMessage);
    fclose(f);
}

static void FileSink_Destroy(ILogSink *self)
{
    free(self);
}

ILogSink *CreateFileSink(const char *fullPath)
{
    if (!fullPath || fullPath[0] == '\0') return NULL;
    if (strlen(fullPath) >= MAX_PATH) return NULL;

    FileSink *fileSink = (FileSink *)malloc(sizeof(FileSink));
    if (!fileSink) return NULL;

    fileSink->base.Write = FileSink_Write;
    fileSink->base.Destroy = FileSink_Destroy;
    strcpy(fileSink->path, fullPath);

    return &fileSink->base;
}
