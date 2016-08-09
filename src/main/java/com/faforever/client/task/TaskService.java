package com.faforever.client.task;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * Enqueues and runs tasks in background. Services that need to run a task (tasks that finish, not long-running
 * background jobs) in background should always submit them to this service.
 * <p>
 * There are different queues for different kind of tasks. For every queue, only one task is executed at a time.
 */
public interface TaskService {

  /**
   * Submits a task for execution in background.
   * @param <T> the task's result type
   * @param task the task to execute
   */
  <T extends CompletableTask> T submitTask(T task);

  ObservableList<Task<?>> getActiveTasks();
}
