package com.faforever.client.taskqueue;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;

/**
 * Enqueues and runs tasks in background. Services that need to run a task (tasks that finish, not long-running
 * background jobs) in background should always submit them to this service.
 */
public interface TaskQueueService {

  <T> void submitTask(PriorityAwareTask<T> task);

  void addChangeListener(ListChangeListener<? super Task<?>> listener);
}
