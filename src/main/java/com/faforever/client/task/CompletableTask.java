package com.faforever.client.task;

import com.faforever.client.util.Assert;
import javafx.concurrent.Task;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public abstract class CompletableTask<V> extends Task<V> implements PrioritizedCompletableTask<V> {

  private final CompletableFuture<V> future;
  private Priority priority;

  public CompletableTask(Priority priority) {
    this.priority = priority;
    this.future = new CompletableFuture<>();
    setOnCancelled(event -> future.cancel(true));
    setOnFailed(event -> future.completeExceptionally(getException()));
    setOnSucceeded(event -> future.complete(getValue()));
  }

  @Override
  public CompletableFuture<V> getFuture() {
    return future;
  }

  @Override
  public Mono<V> getMono() {
    return Mono.fromFuture(future);
  }

  @Override
  public int compareTo(@NotNull CompletableTask other) {
    return priority.compareTo(other.priority);
  }

  public void setPriority(Priority priority) {
    Assert.checkNotNullIllegalState(this.priority, "Priority has already been set");
    this.priority = priority;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    future.cancel(mayInterruptIfRunning);
    return super.cancel(mayInterruptIfRunning);
  }

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }
}
