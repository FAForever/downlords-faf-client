package com.faforever.client.util;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public final class ConcurrentUtil {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ConcurrentUtil() {
    // Utility class
  }

  public static <T> void executeInBackground(final Task<T> task) {
    executeInBackground(task, null);
  }

  public static <T> Service<T> executeInBackground(final Task<T> task, final Callback<T> callback) {
    task.setOnSucceeded(event -> {
      if (callback == null) {
        return;
      }

      callback.success((T) event.getSource().getValue());
    });
    task.setOnFailed(event -> {
      Throwable exception = event.getSource().getException();
      logger.warn("Task failed", exception);

      if (callback == null) {
        return;
      }
      callback.error(exception);
    });

    Service<T> service = new Service<T>() {
      @Override
      protected Task<T> createTask() {
        return task;
      }
    };
    service.start();

    return service;
  }
}
