package com.faforever.client.test;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.ui.StageHolder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
//TODO figure out best way to refactor so that tests don't have to be lenient due to unnecessary stubbings spam
@Slf4j
public abstract class UITest extends ApplicationTest {

  static {
    System.setProperty("testfx.robot", "glass");
    System.setProperty("testfx.headless", "true");
    System.setProperty("prism.order", "sw");
    System.setProperty("prism.text", "t2k");
  }

  private final Pane root;
  private Scene scene;
  private Stage stage;

  public UITest() {
    root = new Pane();
    Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
      log.error("Unresolved Throwable in none junit thread, please resolve even if test does not fail", e);
    });
  }

  @Override
  public void start(Stage stage) throws Exception {
    this.stage = stage;
    StageHolder.setStage(stage);

    scene = createScene(stage);
    stage.setScene(scene);

    if (showStage()) {
      stage.show();
    }
  }

  protected Scene createScene(Stage stage) {
    return new Scene(getRoot());
  }

  protected boolean showStage() {
    return true;
  }

  protected Pane getRoot() {
    return root;
  }

  protected Scene getScene() {
    return scene;
  }

  protected Stage getStage() {
    return stage;
  }

  protected void loadFxml(String fileName, Callback<Class<?>, Object> controllerFactory) throws IOException, InterruptedException {
    loadFxml(fileName, controllerFactory, null);
  }

  protected void loadFxml(String fileName, Callback<Class<?>, Object> controllerFactory, Controller<?> controller) throws IOException, InterruptedException {
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
    CountDownLatch latch = new CountDownLatch(1);
    JavaFxUtil.runLater(() -> {
      try {
        loader.load();
      } catch (Exception e) {
        loadExceptionWrapper.setLoadException(e);
      }
      latch.countDown();
    });
    latch.await();
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
    JavaFxUtil.runLater(runnable);
    WaitForAsyncUtils.waitForFxEvents();
  }
}
