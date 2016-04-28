package com.faforever.client.task;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

public class TaskServiceImpl implements TaskService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ObservableList<PrioritizedTask<?>> activeTasks;

  @Resource
  ThreadPoolExecutor threadPoolExecutor;

  private ObservableList<PrioritizedTask<?>> unmodifiableObservableList;

  public TaskServiceImpl() {
    activeTasks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    unmodifiableObservableList = FXCollections.unmodifiableObservableList(activeTasks);
  }

  @Override
  public <T> CompletableFuture<T> submitTask(PrioritizedTask<T> task) {
    CompletableFuture<T> future = new CompletableFuture<>();

    task.setOnFailed(event -> {
      Throwable exception = task.getException();
      logger.warn("Task failed", exception);
      future.completeExceptionally(exception);
      activeTasks.remove(task);
    });
    task.setOnSucceeded(event -> {
      future.complete(task.getValue());
      activeTasks.remove(task);
    });

    activeTasks.add(task);
    threadPoolExecutor.execute(task);

    return future;
  }

  @Override
  public ObservableList<PrioritizedTask<?>> getActiveTasks() {
    return unmodifiableObservableList;
  }

  @PreDestroy
  void preDestroy() {
    threadPoolExecutor.shutdownNow();
  }
}
