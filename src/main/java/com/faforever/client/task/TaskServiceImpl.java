package com.faforever.client.task;

import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
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
  private final Map<TaskGroup, ObservableList<PrioritizedTask<?>>> queueListsByGroup;

  public TaskServiceImpl() {
    queuesByGroup = new HashMap<>();
    queueListsByGroup = new HashMap<>();

    for (TaskGroup taskGroup : TaskGroup.values()) {
      queuesByGroup.put(taskGroup, new PriorityBlockingQueue<>());
      queueListsByGroup.put(taskGroup, FXCollections.observableArrayList());
    }
  }

  @PostConstruct
  void startWorkers() {
    for (TaskGroup taskGroup : TaskGroup.values()) {
      startWorker(taskGroup);
    }
  }

  @Override
  public <T> void submitTask(TaskGroup taskGroup, PrioritizedTask<T> task, Callback<T> callback) {
    ConcurrentUtil.setCallbackOnTask(task, callback);

    queuesByGroup.get(taskGroup).add(task);

    ObservableList<PrioritizedTask<?>> tasks = queueListsByGroup.get(taskGroup);
    tasks.add(task);
    FXCollections.sort(tasks);
  }

  private void startWorker(TaskGroup taskGroup) {
    logger.debug("Starting worker for task group {}", taskGroup);

    PriorityBlockingQueue<PrioritizedTask<?>> queue = queuesByGroup.get(taskGroup);
    ObservableList<PrioritizedTask<?>> queueList = queueListsByGroup.get(taskGroup);

    // FIXME the task group IMMEDIATE should not use the same queueing mechanism
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          PrioritizedTask<?> task = queue.take();
          task.run();
          queueList.remove(task);
        }
        return null;
      }
    });
  }

  @Override
  public <T> void submitTask(TaskGroup taskGroup, PrioritizedTask<T> task) {
    submitTask(taskGroup, task, null);
  }

  @Override
  public void addChangeListener(ListChangeListener<PrioritizedTask<?>> listener, TaskGroup... taskGroups) {
    for (TaskGroup taskGroup : taskGroups) {
      queueListsByGroup.get(taskGroup).addListener(listener);
    }
  }




}
