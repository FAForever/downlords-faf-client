package com.faforever.client.ui.taskbar;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.task.TaskService;
import javafx.beans.Observable;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Worker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.awt.Taskbar;
import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * Updates the progress in the Windows taskbar (Windows 7+), using java.awt.Taskbar API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowsTaskbarProgressUpdater implements InitializingBean {

  private final TaskService taskService;
  private final Executor executorService;
  private final SimpleChangeListener<Number> progressUpdateListener = newValue -> updateTaskbarProgress(newValue.doubleValue());

  private Taskbar taskbar;

  @Override
  public void afterPropertiesSet() {
    if (Taskbar.isTaskbarSupported()) {
      taskbar = Taskbar.getTaskbar();
    } else {
      log.warn("Taskbar API is not supported on this platform");
    }

    JavaFxUtil.addListener(taskService.getActiveWorkers(), (Observable observable) -> onActiveTasksChanged());
  }

  private void onActiveTasksChanged() {
    JavaFxUtil.assertApplicationThread();
    Collection<Worker<?>> runningTasks = taskService.getActiveWorkers();
    if (runningTasks.isEmpty()) {
      updateTaskbarProgress(null);
    } else {
      Worker<?> task = runningTasks.iterator().next();
      JavaFxUtil.addListener(task.progressProperty(), new WeakChangeListener<>(progressUpdateListener));
      updateTaskbarProgress(task.getProgress());
    }
  }

  private void updateTaskbarProgress(@Nullable Double progress) {
    executorService.execute(() -> {
      if (taskbar == null || !taskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
        return;
      }

      try {
        if (progress == null || progress < 0) {
          // Fallback: set to 0 when no progress is available
          taskbar.setProgressValue(0);
        } else {
          int clampedProgress = Math.max(0, Math.min(100, (int) (progress * 100)));
          taskbar.setProgressValue(clampedProgress);
        }
      } catch (UnsupportedOperationException e) {
        log.warn("Taskbar progress not supported: {}", e.getMessage());
      }
    });
  }
}
