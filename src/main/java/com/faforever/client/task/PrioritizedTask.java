package com.faforever.client.task;

import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.util.concurrent.RunnableFuture;

public interface PrioritizedTask<V> extends Comparable<CompletableTask>, Worker<V>, Runnable, RunnableFuture<V> {

  void setOnSucceeded(EventHandler<WorkerStateEvent> value);

  void setOnFailed(EventHandler<WorkerStateEvent> value);

  V getValue();

  Throwable getException();

  String getTitle();

  boolean cancel(boolean mayInterruptIfRunning);
}
