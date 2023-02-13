package com.faforever.client.fx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class JavaFxService {

  private static final Scheduler FX_THREAD_SCHEDULER = Schedulers.fromExecutor(JavaFxUtil::runLater);

  public Scheduler getFxThreadScheduler() {
    return FX_THREAD_SCHEDULER;
  }
}
