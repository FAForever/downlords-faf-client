package com.faforever.client.taskqueue;

import com.faforever.client.util.Callback;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;

/**
 * Enqueues and runs tasks in background. Services that need to run a task (tasks that finish, not long-running
 * background jobs) in background should always submit them to this service.
 */
public interface TaskQueueService {

  /**
   * Submits a task for execution in background and calls the specified callback on completion. The task must not have
   * {@link Task#onSucceededProperty()} or {@link Task#onFailedProperty()} set.
   *
   * @param task the task to execute
   * @param callback the callback to call on completion
   * @param <T> the task's result type
   */
  <T> void submitTask(PrioritizedTask<T> task, Callback<T> callback);

  void addChangeListener(ListChangeListener<? super Task<?>> listener);
}
