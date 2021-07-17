package com.faforever.client.task;

import javafx.concurrent.Worker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RunnableFuture;

public interface PrioritizedCompletableTask<V> extends Comparable<CompletableTask<V>>, Worker<V>, RunnableFuture<V> {

  V getValue();

  Throwable getException();

  String getTitle();

  boolean cancel(boolean mayInterruptIfRunning);

  CompletableFuture<V> getFuture();
}
