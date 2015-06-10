package com.faforever.client.taskqueue;

import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import javax.annotation.PostConstruct;
import java.util.concurrent.PriorityBlockingQueue;

public class TaskQueueServiceImpl implements TaskQueueService {

  /**
   * Since there is no observable queue in JavaFX, this list serves as target for listeners.
   */
  private final ObservableList<PrioritizedTask<?>> list;
  private PriorityBlockingQueue<PrioritizedTask<?>> queue;

  public TaskQueueServiceImpl() {
    queue = new PriorityBlockingQueue<>();
    list = FXCollections.observableArrayList();
  }

  @Override
  public <T> void submitTask(PrioritizedTask<T> task, Callback<T> callback) {
    ConcurrentUtil.setCallbackOnTask(task, callback);
    queue.add(task);
    list.add(task);
    FXCollections.sort(list);
  }

  @Override
  public void addChangeListener(ListChangeListener<? super Task<?>> listener) {
    list.addListener(listener);
  }

  @PostConstruct
  void start() {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          PrioritizedTask<?> task = queue.take();
          task.run();
          list.remove(task);
        }
        return null;
      }
    });
  }
}
