package com.faforever.client.task;

import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

public class TaskServiceImpl implements TaskService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Holds one queue for each TaskGroup.
   */
  private final PriorityBlockingQueue<PrioritizedTask<?>> queue;

  private final Collection<OnTasksUpdatedListener> onTasksUpdatedListeners;

  public TaskServiceImpl() {
    queue = new PriorityBlockingQueue<>();
    onTasksUpdatedListeners = new HashSet<>();
  }

  @PostConstruct
  void startWorkers() {
    logger.debug("Starting task queue");

    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          PrioritizedTask<?> task = queue.take();
          task.run();
          onTasksUpdatedListeners.forEach(OnTasksUpdatedListener::onTasksUpdated);
        }
        return null;
      }
    });
  }

  @Override
  public <T> void submitTask(PrioritizedTask<T> task, Callback<T> callback) {
    ConcurrentUtil.setCallbackOnTask(task, callback);
    queue.add(task);
    onTasksUpdatedListeners.forEach(OnTasksUpdatedListener::onTasksUpdated);
  }

  @Override
  public <T> void submitTask(PrioritizedTask<T> task) {
    submitTask(task, null);
  }

  @Override
  public void addListener(OnTasksUpdatedListener listener) {
    onTasksUpdatedListeners.add(listener);
  }

  @Override
  public Collection<PrioritizedTask<?>> getRunningTasks() {
    Collection<PrioritizedTask<?>> tasks = new ArrayList<>();
    queue.forEach(tasks::add);
    return tasks;
  }
}
