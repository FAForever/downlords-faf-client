package com.faforever.client.fx;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;

@Slf4j
@Component
public class FxApplicationThreadExecutor implements Executor {

  private final Scheduler fxApplicationScheduler = Schedulers.fromExecutor(this);

  public void execute(Runnable runnable) {
    if (Platform.isFxApplicationThread()) {
      try {
        runnable.run();
      } catch (Exception e) {
        log.error("Uncaught Application Thread Error", e);
      }
    } else {
      Platform.runLater(runnable);
    }
  }

  public Scheduler asScheduler() {
    return fxApplicationScheduler;
  }
}
