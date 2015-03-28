package com.faforever.client.util;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
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

  public static <T> void executeInBackground(final Task<T> task, final Callback<T> callback) {
    task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
      @Override
      public void handle(WorkerStateEvent event) {
        if (callback == null) {
          return;
        }

        Throwable exception = event.getSource().getException();
        if (exception != null) {
          callback.error(exception);
        } else {
          callback.success((T) event.getSource().getValue());
        }
      }
    });
    task.setOnFailed(new EventHandler<WorkerStateEvent>() {
      @Override
      public void handle(WorkerStateEvent event) {
        Throwable exception = event.getSource().getException();
        logger.warn("Task failed", exception);
        callback.error(exception);
      }
    });

    new Service<T>() {
      @Override
      protected Task<T> createTask() {
        return task;
      }
    }.start();
  }
}
