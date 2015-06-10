package com.faforever.client.taskqueue;

import com.faforever.client.util.ConcurrentUtil;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;

import javax.annotation.PostConstruct;
import java.util.concurrent.PriorityBlockingQueue;

public class TaskQueueServiceImpl implements TaskQueueService {

  /**
   * Since there is no observable queue in JavaFX, this list serves as target for listeners.
   */
  private final ObservableList<Task<?>> list;
  private PriorityBlockingQueue<Task<?>> queue;

  public TaskQueueServiceImpl() {
    queue = new PriorityBlockingQueue<>();
    list = new SortedList<>(FXCollections.observableArrayList());
  }

  @Override
  public <T> void submitTask(PriorityAwareTask<T> task) {
    queue.add(task);
    list.add(task);
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

          Task<?> task = queue.take();
          try {
            task.run();
          } finally {
            list.remove(task);
          }
        }
        return null;
      }
    });
  }
}
