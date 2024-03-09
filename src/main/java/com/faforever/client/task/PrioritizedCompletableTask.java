package com.faforever.client.task;

import javafx.concurrent.Worker;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RunnableFuture;

public interface PrioritizedCompletableTask<V> extends Comparable<CompletableTask<V>>, Worker<V>, RunnableFuture<V> {

  @Override
  V getValue();

  @Override
  Throwable getException();

  @Override
  String getTitle();

  @Override
  boolean cancel(boolean mayInterruptIfRunning);

  CompletableFuture<V> getFuture();

  Mono<V> getMono();
}
