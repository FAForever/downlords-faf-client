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

  /**
   * Executes the given task in background. A callback is automatically added that logs to error if the task has
   * failed.
   *
   * @return the {@link Service} the specified task has been started in.
   */
  public static <T> Service<T> executeInBackground(final Task<T> task) {
    return executeInBackground(task, new Callback<T>() {
      @Override
      public void success(T result) {

      }

      @Override
      public void error(Throwable e) {
        logger.error("Task failed", e);
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Executes the given task in background and calls the specified callback when finished. The callback is always called
   * on the FX application thread.
   *
   * @return the {@link Service} the specified task has been started in.
   */
  public static <T> Service<T> executeInBackground(final Task<T> task, final Callback<T> callback) {
    setCallbackOnTask(task, callback);

    Service<T> service = new Service<T>() {
      @Override
      protected Task<T> createTask() {
        return task;
      }
    };
    service.start();

    return service;
  }

  /**
   * Sets the specified callback to the task's onSucceeded and onFailed methods. If the callback is null, this method
   * will return without doing anything.
   *
   * @throws IllegalStateException of {@code onSucceeded} or {@code onFailed} is already specified
   */
  @SuppressWarnings("unchecked")
  public static <T> void setCallbackOnTask(Task<T> task, Callback<T> callback) {
    if (callback == null) {
      return;
    }

    if (task.getOnSucceeded() != null) {
      throw new IllegalStateException("onSucceeded has already been set but should be null in order to use a callback");
    }
    if (task.getOnFailed() != null) {
      throw new IllegalStateException("onFailed has already been set but should be null in order to use a callback");
    }

    task.setOnSucceeded(event -> callback.success((T) event.getSource().getValue()));

    task.setOnFailed(event -> {
      Throwable exception = event.getSource().getException();
      logger.warn("Task failed", exception);
      callback.error(exception);
    });
  }
}
