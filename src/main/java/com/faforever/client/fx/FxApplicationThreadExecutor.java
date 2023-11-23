package com.faforever.client.fx;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/* Guarantees tasks are executed on the JavaFX Application Thread
 */
@Slf4j
@Component
public class FxApplicationThreadExecutor implements Executor {

  private final Scheduler fxApplicationScheduler = Schedulers.fromExecutor(this);

  @Override
  public void execute(Runnable runnable) {
    if (Platform.isFxApplicationThread()) {
      try {
        runnable.run();
      } catch (Exception e) {
        log.error("Uncaught Application Thread Error", e);
      }
    } else {
      runLater(runnable);
    }
  }

  public void runLater(Runnable runnable) {
    Platform.runLater(runnable);
  }

  public void executeAndWait(Runnable runnable) {
    CountDownLatch doneLatch = new CountDownLatch(1);
    execute(() -> {
      try {
        runnable.run();
      } finally {
        doneLatch.countDown();
      }
    });

    try {
      doneLatch.await();
    } catch (InterruptedException e) {
      log.error("A thread interrupted", e);
    }
  }

  public Scheduler asScheduler() {
    return fxApplicationScheduler;
  }
}
