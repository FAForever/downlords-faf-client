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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.PriorityBlockingQueue;

public class TaskServiceImpl implements TaskService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Holds one queue for each TaskGroup.
   */
  private final Map<TaskGroup, PriorityBlockingQueue<PrioritizedTask<?>>> queuesByGroup;

  /**
   * Since there is no observable queue in JavaFX, these lists serve as target for queue listeners.
   */
  private final Map<TaskGroup, SortedSet<PrioritizedTask<?>>> queueListsByGroup;

  private final Map<TaskGroup, Collection<OnTasksUpdatedListener>> onTasksUpdatedListeners;

  public TaskServiceImpl() {
    queuesByGroup = new HashMap<>();
    queueListsByGroup = new HashMap<>();
    onTasksUpdatedListeners = new HashMap<>();

    for (TaskGroup taskGroup : TaskGroup.values()) {
      queuesByGroup.put(taskGroup, new PriorityBlockingQueue<>());
      queueListsByGroup.put(taskGroup, new TreeSet<>());
      onTasksUpdatedListeners.put(taskGroup, new ArrayList<>());
    }
  }

  @PostConstruct
  void startWorkers() {
    for (TaskGroup taskGroup : TaskGroup.values()) {
      startWorker(taskGroup);
    }
  }

  private void startWorker(TaskGroup taskGroup) {
    logger.debug("Starting worker for task group {}", taskGroup);

    PriorityBlockingQueue<PrioritizedTask<?>> queue = queuesByGroup.get(taskGroup);
    SortedSet<PrioritizedTask<?>> queueList = queueListsByGroup.get(taskGroup);

    // FIXME the task group IMMEDIATE should not use the same queueing mechanism
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          PrioritizedTask<?> task = queue.take();
          task.run();
          queueList.remove(task);
          onTasksUpdatedListeners.get(taskGroup).forEach(OnTasksUpdatedListener::onTasksUpdated);
        }
        return null;
      }
    });
  }

  @Override
  public <T> void submitTask(TaskGroup taskGroup, PrioritizedTask<T> task, Callback<T> callback) {
    ConcurrentUtil.setCallbackOnTask(task, callback);

    queuesByGroup.get(taskGroup).add(task);
    queueListsByGroup.get(taskGroup).add(task);

    onTasksUpdatedListeners.get(taskGroup).forEach(OnTasksUpdatedListener::onTasksUpdated);
  }

  @Override
  public <T> void submitTask(TaskGroup taskGroup, PrioritizedTask<T> task) {
    submitTask(taskGroup, task, null);
  }

  @Override
  public void addListener(OnTasksUpdatedListener listener, TaskGroup... taskGroups) {
    for (TaskGroup taskGroup : taskGroups) {
      onTasksUpdatedListeners.get(taskGroup).add(listener);
    }
  }

  @Override
  public Collection<PrioritizedTask<?>> getRunningTasks() {
    Collection<PrioritizedTask<?>> tasks = new ArrayList<>();
    queueListsByGroup.values().forEach(tasks::addAll);
    return tasks;
  }
}
