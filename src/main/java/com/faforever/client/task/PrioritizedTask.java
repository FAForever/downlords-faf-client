package com.faforever.client.task;

import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public interface PrioritizedTask<V> extends Comparable<AbstractPrioritizedTask>, Worker<V>, Runnable {

  EventHandler<WorkerStateEvent> getOnSucceeded();

  void setOnSucceeded(EventHandler<WorkerStateEvent> value);

  EventHandler<WorkerStateEvent> getOnFailed();

  void setOnFailed(EventHandler<WorkerStateEvent> value);

  V getValue();

  Throwable getException();

  String getTitle();

  boolean cancel(boolean mayInterruptIfRunning);
}
