package com.faforever.client.task;

import com.faforever.client.util.Callback;
import javafx.concurrent.Task;

import java.util.Collection;

/**
 * Enqueues and runs tasks in background. Services that need to run a task (tasks that finish, not long-running
 * background jobs) in background should always submit them to this service.
 * <p>
 * There are different queues for different kind of tasks. For every queue, only one task is executed at a time.
 */
public interface TaskService {

  /**
   * Submits a task for execution in background and calls the specified callback on completion. The task must not have
   * {@link Task#onSucceededProperty()} or {@link Task#onFailedProperty()} set.
   *
   * @param <T> the task's result type
   * @param taskGroup the task group this task should be assigned to
   * @param task the task to execute
   * @param callback the callback to call on completion
   */
  <T> void submitTask(TaskGroup taskGroup, PrioritizedTask<T> task, Callback<T> callback);

  /**
   * Submits a task for execution in background.
   *
   * @param <T> the task's result type
   * @param taskGroup the task group this task should be assigned to
   * @param task the task to execute
   */
  <T> void submitTask(TaskGroup taskGroup, PrioritizedTask<T> task);

  void addListener(OnTasksUpdatedListener listener, TaskGroup... taskGroups);

  Collection<PrioritizedTask<?>> getRunningTasks();
}
