package com.faforever.client.task;

import java.util.concurrent.Future;

public class BackgroundTask<V> extends CompletableTask<V> {

  private final String title;
  private final Future<V> task;

  public BackgroundTask(String titleInStatusBar, Future<V> task) {
    super(Priority.MEDIUM);
    this.title = titleInStatusBar;
    this.task = task;
  }

  @Override
  protected V call() throws Exception {
    updateTitle(title);
    return task.get();
  }
}
