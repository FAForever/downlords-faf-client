package com.faforever.client.ui.taskbar;

import com.faforever.client.FafClientApplication;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.task.TaskService;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.control.ProgressIndicator;
import lombok.extern.slf4j.Slf4j;
import org.bridj.Pointer;
import org.bridj.PointerIO;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Updates the progress in the Windows 7+ task bar, if available.
 */
@Slf4j
@Component
@Profile(FafClientApplication.PROFILE_WINDOWS)
public class WindowsTaskbarProgressUpdater implements InitializingBean {

  private final TaskService taskService;
  private final Executor executorService;
  private final ChangeListener<Number> progressUpdateListener;

  private ITaskbarList3 taskBarList;
  private Pointer<Integer> taskBarPointer;

  public WindowsTaskbarProgressUpdater(TaskService taskService, ExecutorService executorService) {
    this.taskService = taskService;
    this.executorService = executorService;
    progressUpdateListener = (observable1, oldValue, newValue) -> updateTaskbarProgress(newValue.doubleValue());
  }

  @Override
  public void afterPropertiesSet() {
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

  @SuppressWarnings("unchecked")
  public void initTaskBar() {
    try {
      executorService.execute(() -> {
        try {
          taskBarList = COMRuntime.newInstance(ITaskbarList3.class);
        } catch (ClassNotFoundException e) {
          log.warn("Could not initialize TaskBar", e);
        }
      });
      long hwndVal = com.sun.jna.Pointer.nativeValue(JavaFxUtil.getNativeWindow());
      taskBarPointer = Pointer.pointerToAddress(hwndVal, (PointerIO) null);
    } catch (NoClassDefFoundError e) {
      taskBarPointer = null;
    }
  }

  @SuppressWarnings("unchecked")
  private void updateTaskbarProgress(@Nullable Double progress) {
    executorService.execute(() -> {
      if (taskBarPointer == null || taskBarList == null) {
        return;
      }

      if (progress == null) {
        taskBarList.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS);
      } else if (progress != ProgressIndicator.INDETERMINATE_PROGRESS) {
        taskBarList.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_NORMAL);
        taskBarList.SetProgressValue(taskBarPointer, (int) (progress * 100), 100);
      }
    });
  }
}
