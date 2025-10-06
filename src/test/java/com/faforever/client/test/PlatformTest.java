package com.faforever.client.test;

import com.faforever.client.fx.AttachedUtil;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import io.github.sheikah45.fx2j.api.Fx2jLoader;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith({MockitoExtension.class})
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

  protected BooleanProperty attached = new SimpleBooleanProperty(true);

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
    } catch (IllegalStateException _) {
    }
  }

  protected void loadFxml(String fileName,
                          Function<Class<?>, Object> controllerFactory) throws IOException, InterruptedException {
    loadFxml(fileName, controllerFactory, null);
  }

  protected void loadFxml(String fileName, Function<Class<?>, Object> controllerFactory,
                          Controller<?> controller) throws IOException, InterruptedException {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.messages");

    @Data
    class ExceptionWrapper {
      private Exception loadException;
    }

    ExceptionWrapper loadExceptionWrapper = new ExceptionWrapper();

    Fx2jLoader loader = new Fx2jLoader();
    loader.setLocation(getThemeFileUrl(fileName));
    loader.setResources(new MessageSourceResourceBundle(messageSource, Locale.US));
    loader.setControllerFactory(controllerFactory);
    if (controller != null) {
      loader.setController(controller);
    }
    fxApplicationThreadExecutor.executeAndWait(() -> {
      try (MockedStatic<AttachedUtil> mockedStatic = mockStatic(AttachedUtil.class)) {
        mockedStatic.when(() -> AttachedUtil.attachedProperty(any(MenuItem.class))).thenReturn(attached);
        mockedStatic.when(() -> AttachedUtil.attachedProperty(any(Node.class))).thenReturn(attached);
        mockedStatic.when(() -> AttachedUtil.attachedProperty(any(Tab.class))).thenReturn(attached);
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
    waitFxEvents();
  }

  protected void waitFxEvents() {
    WaitForAsyncUtils.waitForFxEvents();
  }

  protected void reinitialize(Controller<?> controller) {
    fxApplicationThreadExecutor.executeAndWait(() -> {
      try (MockedStatic<AttachedUtil> mockedStatic = mockStatic(AttachedUtil.class)) {
        mockedStatic.when(() -> AttachedUtil.attachedProperty(any(MenuItem.class))).thenReturn(attached);
        mockedStatic.when(() -> AttachedUtil.attachedProperty(any(Node.class))).thenReturn(attached);
        mockedStatic.when(() -> AttachedUtil.attachedProperty(any(Tab.class))).thenReturn(attached);
        controller.initialize();
      }
    });
  }
}
