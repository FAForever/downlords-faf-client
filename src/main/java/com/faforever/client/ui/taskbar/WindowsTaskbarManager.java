package com.faforever.client.ui.taskbar;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.task.TaskService;
import javafx.beans.Observable;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.control.ProgressIndicator;
import org.bridj.Pointer;
import org.bridj.PointerIO;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.Executor;

import static com.github.nocatch.NoCatch.noCatch;

/**
 * Updates the progress in the Windows 7+ task bar, if available.
 */
@Component
@Profile("windows7+")
public class WindowsTaskbarManager {

  private final TaskService taskService;
  private final Executor threadPoolExecutor;

  private ITaskbarList3 taskBarList;
  private Pointer<Integer> taskBarPointer;

  public WindowsTaskbarManager(TaskService taskService, Executor threadPoolExecutor) {
    this.taskService = taskService;
    this.threadPoolExecutor = threadPoolExecutor;
  }

  public void initialize() {
    JavaFxUtil.addListener(taskService.getActiveWorkers(), (Observable observable) -> onActiveTasksChanged());
    initTaskBar();
  }

  private void onActiveTasksChanged() {
    Collection<Worker<?>> runningTasks = taskService.getActiveWorkers();
    if (runningTasks.isEmpty()) {
      updateTaskbarProgress(null);
    } else {
      Worker<?> task = runningTasks.iterator().next();
      updateTaskbarProgress(task.getProgress());
      JavaFxUtil.addListener(task.progressProperty(), new WeakChangeListener<>(
          (observable1, oldValue, newValue) -> updateTaskbarProgress(newValue.doubleValue()))
      );
    }
  }

  @SuppressWarnings("unchecked")
  private void initTaskBar() {
    try {
      threadPoolExecutor.execute(() -> noCatch(() -> taskBarList = COMRuntime.newInstance(ITaskbarList3.class)));
      long hwndVal = JavaFxUtil.getNativeWindow();
      taskBarPointer = Pointer.pointerToAddress(hwndVal, (PointerIO) null);
    } catch (NoClassDefFoundError e) {
      taskBarPointer = null;
    }
  }

  @SuppressWarnings("unchecked")
  private void updateTaskbarProgress(@Nullable Double progress) {
    if (taskBarPointer == null || taskBarList == null) {
      return;
    }
    threadPoolExecutor.execute(() -> {
      if (progress == null) {
        taskBarList.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS);
      } else if (progress == ProgressIndicator.INDETERMINATE_PROGRESS) {
        taskBarList.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_INDETERMINATE);
      } else {
        taskBarList.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_NORMAL);
        taskBarList.SetProgressValue(taskBarPointer, (int) (progress * 100), 100);
      }
    });
  }
}
