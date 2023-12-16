package com.faforever.client.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionException;

@Slf4j
public final class ConcurrentUtil {
  public static Throwable unwrapIfCompletionException(Throwable throwable) {
    return throwable instanceof CompletionException completionException ? unwrapIfCompletionException(
        completionException.getCause()) : throwable;
  }

  public static CompletionException wrapInCompletionExceptionIfNecessary(Throwable throwable) {
    return throwable instanceof CompletionException completionException ? completionException : new CompletionException(
        throwable);
  }
}
