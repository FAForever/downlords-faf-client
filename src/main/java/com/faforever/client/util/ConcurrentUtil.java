package com.faforever.client.util;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionException;

@Slf4j
public final class ConcurrentUtil {

  /**
   * Executes the given task in background and calls the specified callback when finished. The callback is always called
   * on the FX application thread.
   *
   * @return the {@link Service} the specified task has been started in.
   */
  @SuppressWarnings("unchecked")
  // TODO this needs to be removed
  public static <T> Service<T> executeInBackground(final Worker<T> worker) {
    Service<T> service = new Service<T>() {
      @Override
      protected Task<T> createTask() {
        return (Task<T>) worker;
      }
    };
    service.setOnFailed(event -> log.error("Task failed", event.getSource().getException()));
    service.start();

    return service;
  }

  public static Throwable unwrapIfCompletionException(Throwable throwable) {
    return throwable instanceof CompletionException ? throwable.getCause() : throwable;
  }
}
