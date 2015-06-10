package com.faforever.client.taskqueue;

import javafx.concurrent.Task;
import org.jetbrains.annotations.NotNull;

public abstract class PriorityAwareTask<V> extends Task<V> implements Comparable<PriorityAwareTask> {

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }

  private final Priority priority;
  private final long creationTime;

  public PriorityAwareTask() {
    this(Priority.MEDIUM);
  }

  public PriorityAwareTask(Priority priority) {
    this.priority = priority;
    creationTime = System.currentTimeMillis();
  }

  @Override
  public int compareTo(@NotNull PriorityAwareTask other) {
    return priority.compareTo(other.priority);
  }
}
