package com.faforever.client.config;

import com.faforever.client.FafClientApplication;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.legacy.UidService;
import com.faforever.client.test.UITest;
import com.google.common.base.Stopwatch;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.mockito.Mockito.mock;

@Slf4j
public class FafClientApplicationTest extends UITest {

  @Test
  public void testDoesItSmoke() throws Exception {
    runOnFxThreadAndWait(() -> {
      Stopwatch stopwatch = Stopwatch.createStarted();
      try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
        context.getBeanFactory().registerSingleton("hostService", mock(PlatformService.class));
        context.getBeanFactory().registerSingleton("stage", new Stage());
        context.getBeanFactory().registerSingleton("uidService", mock(UidService.class));
        context.register(FafClientApplication.class);
        context.refresh();
        log.debug("Loading application context took {}", stopwatch.stop());
      }
    });
  }
}
