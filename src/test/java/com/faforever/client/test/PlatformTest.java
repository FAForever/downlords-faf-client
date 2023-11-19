package com.faforever.client.test;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.util.Callback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
//TODO figure out best way to refactor so that tests don't have to be lenient due to unnecessary stubbings spam
@Slf4j
public abstract class PlatformTest {

  static {
    System.setProperty("testfx.robot", "glass");
    System.setProperty("testfx.headless", "true");
    System.setProperty("prism.order", "sw");
    System.setProperty("prism.text", "t2k");
    System.setProperty("java.awt.headless", "true");
  }

  @Spy
  protected FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public PlatformTest() {
    Locale.setDefault(Locale.ROOT);

    Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
      log.error("Unresolved Throwable in non junit thread, please resolve even if test does not fail", e);
    });
  }

  @BeforeAll
  public static void setupPlatform() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
    }
  }

  protected void loadFxml(String fileName,
                          Callback<Class<?>, Object> controllerFactory) throws IOException, InterruptedException {
    loadFxml(fileName, controllerFactory, null);
  }

  protected void loadFxml(String fileName, Callback<Class<?>, Object> controllerFactory,
                          Controller<?> controller) throws IOException, InterruptedException {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.messages");

    @Data
    class ExceptionWrapper {
      private Exception loadException;
    }

    ExceptionWrapper loadExceptionWrapper = new ExceptionWrapper();

    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(getThemeFileUrl(fileName));
    loader.setResources(new MessageSourceResourceBundle(messageSource, Locale.US));
    loader.setControllerFactory(controllerFactory);
    if (controller != null) {
      loader.setController(controller);
    }
    fxApplicationThreadExecutor.executeAndWait(() -> {
      try {
        loader.load();
      } catch (Exception e) {
        loadExceptionWrapper.setLoadException(e);
      }
    });
    if (loadExceptionWrapper.getLoadException() != null) {
      throw new RuntimeException("Loading FXML failed", loadExceptionWrapper.getLoadException());
    }
  }

  protected String getThemeFile(String file) {
    return String.format("/%s", file);
  }

  protected URL getThemeFileUrl(String file) throws IOException {
    return new ClassPathResource(getThemeFile(file)).getURL();
  }

  protected void runOnFxThreadAndWait(Runnable runnable) {
    Platform.runLater(runnable);
    WaitForAsyncUtils.waitForFxEvents();
  }
}
