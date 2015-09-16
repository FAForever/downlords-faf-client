package com.faforever.client.task;

import javafx.concurrent.Task;
import org.jetbrains.annotations.NotNull;

public abstract class PrioritizedTask<V> extends Task<V> implements Comparable<PrioritizedTask> {

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }

  private Priority priority;

  public PrioritizedTask() {
  }

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

  public void setPriority(Priority priority) {
    if (this.priority != null) {
      throw new IllegalStateException("Priority has already been set");
    }
    this.priority = priority;
  }
}
