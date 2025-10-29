package com.faforever.client.task;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class SimpleTask<V> extends CompletableTask<V> {

  private final Supplier<? extends CompletableFuture<V>> futureSupplier;

  public SimpleTask(Supplier<? extends CompletableFuture<V>> futureSupplier) {
    this(null, futureSupplier);
  }

  public SimpleTask(String title, Supplier<? extends CompletableFuture<V>> futureSupplier) {
    this.futureSupplier = futureSupplier;
    updateTitle(StringUtils.defaultIfBlank(title, StringUtils.EMPTY));
  }

  @Override
  protected V call() throws Exception {
    Future<V> task = futureSupplier.get();
    return task.get();
  }
}