package com.faforever.client.util;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public final class ConcurrentUtil {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ConcurrentUtil() {
    // Utility class
  }

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
    service.setOnFailed(event -> logger.error("Task failed", event.getSource().getException()));
    service.start();

    return service;
  }
}
