package com.faforever.client.task;

import java.util.concurrent.CompletableFuture;

public class BackgroundTask<V> extends CompletableTask<V> {

  private final String title;

  public BackgroundTask(String taskTitle, CompletableFuture<V> task, Priority priority) {
    super(priority, task);
    this.title = taskTitle;
  }

  @Override
  protected V call() throws Exception {
    updateTitle(title);
    return getFuture().join();
  }
}