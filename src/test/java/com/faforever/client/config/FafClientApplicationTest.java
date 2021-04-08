package com.faforever.client.config;

import com.faforever.client.FafClientApplication;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.legacy.UidService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.base.Stopwatch;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class FafClientApplicationTest extends AbstractPlainJavaFxTest {

  @Test
  public void testDoesItSmoke() throws Exception {
    WaitForAsyncUtils.asyncFx(() -> {
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
    WaitForAsyncUtils.waitForFxEvents();
  }
}
