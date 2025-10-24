package com.faforever.client.task;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.i18n.I18n;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Enqueues and runs tasks in background. Services that need to run a task (tasks that finish, not long-running
 * background jobs) in background should always submit them to this service.
 * <p>
 * There are different queues for different kind of tasks. For every queue, only one task is executed at a time.
 */
@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

  private final ExecutorService taskExecutor;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final I18n i18n;

  private final ObservableList<Worker<?>> activeTasks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
  private final ObservableList<Worker<?>> unmodifiableObservableList = FXCollections.unmodifiableObservableList(activeTasks);

  /**
   * Submits a task for execution in background.
   *
   * @param <T> the task's result type
   * @param task the task to execute
   */
  public <T extends PrioritizedCompletableTask<?>> T submitTask(T task) {
    task.getFuture().whenComplete((_, throwable) -> {
      fxApplicationThreadExecutor.execute(() -> activeTasks.remove(task));
      if (throwable != null) {
        log.error("Task failed", throwable);
      }
    });
    fxApplicationThreadExecutor.execute(() -> {
      activeTasks.add(task);
      taskExecutor.execute(task);
    });

    return task;
  }

  public <V> CompletableFuture<V> submitFutureTask(Supplier<CompletableFuture<V>> task) {
    return submitFutureTask(null, task);
  }

  public <V> CompletableFuture<V> submitFutureTask(String titleI18nKey, Supplier<CompletableFuture<V>> task) {
    String title = titleI18nKey != null ? i18n.get(titleI18nKey) : null;
    return submitTask(new SimpleTask<>(title, task)).getFuture();
  }

  public ObservableList<Worker<?>> getActiveWorkers() {
    return unmodifiableObservableList;
  }
}
