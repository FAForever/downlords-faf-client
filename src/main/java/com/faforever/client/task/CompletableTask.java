package com.faforever.client.task;

import javafx.concurrent.Worker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RunnableFuture;

public interface CompletableTask<V> extends Worker<V>, RunnableFuture<V> {
  CompletableFuture<V> getFuture();
}
