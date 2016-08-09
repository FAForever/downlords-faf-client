package com.faforever.client.task;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadPoolExecutor;

public class TaskServiceImpl implements TaskService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ObservableList<Task<?>> activeTasks;

  @Resource
  ThreadPoolExecutor threadPoolExecutor;

  private ObservableList<Task<?>> unmodifiableObservableList;

  public TaskServiceImpl() {
    activeTasks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    unmodifiableObservableList = FXCollections.unmodifiableObservableList(activeTasks);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends CompletableTask> T submitTask(T task) {
    task.getFuture().whenComplete((o, throwable) -> {
      activeTasks.remove(task);
      if (throwable != null) {
        logger.warn("Task failed", (Throwable) throwable);
      }
    });

    activeTasks.add(task);
    threadPoolExecutor.execute(task);

    return task;
  }

  @Override
  public ObservableList<Task<?>> getActiveTasks() {
    return unmodifiableObservableList;
  }

  @PreDestroy
  void preDestroy() {
    threadPoolExecutor.shutdownNow();
  }
}
