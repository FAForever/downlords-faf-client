package com.faforever.client.task;

import javafx.concurrent.Task;
import org.jetbrains.annotations.NotNull;

public abstract class PrioritizedTask<V> extends Task<V> implements Comparable<PrioritizedTask> {

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }

  private final Priority priority;

  public PrioritizedTask(String title) {
    this(title, Priority.MEDIUM);
  }

  public PrioritizedTask(String title, Priority priority) {
    this.priority = priority;
    updateTitle(title);
  }

  @Override
  public int compareTo(@NotNull PrioritizedTask other) {
    return priority.compareTo(other.priority);
  }
}
