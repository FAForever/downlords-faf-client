package com.faforever.client.task;

import javafx.concurrent.Task;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPrioritizedTask<V> extends Task<V> implements PrioritizedTask<V> {

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }

  private Priority priority;

  public AbstractPrioritizedTask(Priority priority) {
    this.priority = priority;
  }

  @Override
  public int compareTo(@NotNull AbstractPrioritizedTask other) {
    return priority.compareTo(other.priority);
  }

  public void setPriority(Priority priority) {
    if (this.priority != null) {
      throw new IllegalStateException("Priority has already been set");
    }
    this.priority = priority;
  }
}
