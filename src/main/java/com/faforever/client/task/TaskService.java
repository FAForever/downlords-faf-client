package com.faforever.client.task;

import com.faforever.client.fx.JavaFxUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

/**
 * Enqueues and runs tasks in background. Services that need to run a task (tasks that finish, not long-running
 * background jobs) in background should always submit them to this service.
 * <p>
 * There are different queues for different kind of tasks. For every queue, only one task is executed at a time.
 */
@Lazy
@Service
@RequiredArgsConstructor
public class TaskService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ExecutorService executorService;
  private final ObservableList<Worker<?>> activeTasks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

  private final ObservableList<Worker<?>> unmodifiableObservableList = FXCollections.unmodifiableObservableList(activeTasks);

  /**
   * Submits a task for execution in background.
   * @param <T> the task's result type
   * @param task the task to execute
   */
  @SuppressWarnings("unchecked")
  public <T extends PrioritizedCompletableTask> T submitTask(T task) {
    task.getFuture().whenComplete((o, throwable) -> {
      activeTasks.remove(task);
      if (throwable != null) {
        logger.warn("Task failed", (Throwable) throwable);
      }
    });
    JavaFxUtil.runLater(() -> {
      activeTasks.add(task);
      executorService.execute(task);
    });

    return task;
  }

  public ObservableList<Worker<?>> getActiveWorkers() {
    return unmodifiableObservableList;
  }
}
