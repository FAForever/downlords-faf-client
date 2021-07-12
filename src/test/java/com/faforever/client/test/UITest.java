package com.faforever.client.test;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.vault.VaultEntityController;
import com.github.nocatch.NoCatch.NoCatchRunnable;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static com.github.nocatch.NoCatch.noCatch;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
//TODO figure out best way to refactor so that tests don't have to be lenient due to unnecessary stubbings spam
@Slf4j
public abstract class UITest extends ApplicationTest {

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
    return new Scene(getRoot(), 1, 1);
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

  protected void loadFxml(String fileName, Callback<Class<?>, Object> controllerFactory) throws IOException {
    loadFxml(fileName, controllerFactory, null);
  }

  protected void loadFxml(String fileName, Callback<Class<?>, Object> controllerFactory, VaultEntityController<?> controller) throws IOException {
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
        noCatch((Callable<Object>) loader::load);
      } catch (Exception e) {
        loadExceptionWrapper.setLoadException(e);
      }
      latch.countDown();
    });
    noCatch((NoCatchRunnable) latch::await);
    if (loadExceptionWrapper.getLoadException() != null) {
      throw new RuntimeException("Loading fxm failed", loadExceptionWrapper.getLoadException());
    }
  }

  protected String getThemeFile(String file) {
    return String.format("/%s", file);
  }

  protected URL getThemeFileUrl(String file) {
    return noCatch(() -> new ClassPathResource(getThemeFile(file)).getURL());
  }

  protected void runOnFxThreadAndWait(Runnable runnable) {
    JavaFxUtil.runLater(runnable);
    WaitForAsyncUtils.waitForFxEvents();
  }
}
