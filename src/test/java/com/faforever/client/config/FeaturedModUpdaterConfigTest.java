package com.faforever.client.config;

import com.faforever.client.FafClientApplication;
import com.faforever.client.fx.PlatformService;
import com.google.common.base.Stopwatch;
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.invoke.MethodHandles;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class FeaturedModUpdaterConfigTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testDoesItSmoke() throws Exception {
    WaitForAsyncUtils.asyncFx(() -> {
      Stopwatch stopwatch = Stopwatch.createStarted();
      try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
        context.getBeanFactory().registerSingleton("hostService", mock(PlatformService.class));
        context.getBeanFactory().registerSingleton("stage", new Stage());
        context.register(FafClientApplication.class);
        context.refresh();
        logger.debug("Loading application context took {}", stopwatch.stop());
      }
    });
    WaitForAsyncUtils.waitForFxEvents();
  }
}
