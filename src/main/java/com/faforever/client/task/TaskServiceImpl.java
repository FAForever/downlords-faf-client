package com.faforever.client.task;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class TaskServiceImpl implements TaskService {

  private final ObservableList<PrioritizedTask<?>> activeTasks;

  @Resource
  ScheduledThreadPoolExecutor threadPoolExecutor;

  private ObservableList<PrioritizedTask<?>> unmodifiableObservableList;

  public TaskServiceImpl() {
    activeTasks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    unmodifiableObservableList = FXCollections.unmodifiableObservableList(activeTasks);
  }

  @Override
  public <T> CompletableFuture<T> submitTask(PrioritizedTask<T> task) {
    CompletableFuture<T> future = new CompletableFuture<>();

    task.setOnFailed(event -> {
      future.completeExceptionally(task.getException());
      activeTasks.remove(task);
    });
    task.setOnSucceeded(event -> {
      future.complete(task.getValue());
      activeTasks.remove(task);
    });

    activeTasks.add(task);
    threadPoolExecutor.submit(task);

    return future;
  }

  @Override
  public ObservableList<PrioritizedTask<?>> getActiveTasks() {
    return unmodifiableObservableList;
  }
}
