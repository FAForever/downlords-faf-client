package com.faforever.client.config;

import com.faforever.client.fx.HostService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.stage.Stage;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import static org.mockito.Mockito.mock;

public class ServiceConfigTest extends AbstractPlainJavaFxTest {

  @Test
  public void testDoesItSmoke() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(20000, () -> {
      try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
        context.getBeanFactory().registerSingleton("hostService", mock(HostService.class));
        context.getBeanFactory().registerSingleton("stage", new Stage());
        context.register(BaseConfig.class, UiConfig.class, ServiceConfig.class, TaskConfig.class, CacheConfig.class, LuceneConfig.class);
        context.refresh();
      }
    });
  }
}
