package com.faforever.client.test;

import com.faforever.client.ui.StageHolder;
import com.faforever.client.vault.VaultEntityController;
import com.github.nocatch.NoCatch.NoCatchRunnable;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.testfx.framework.junit.ApplicationTest;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static com.github.nocatch.NoCatch.noCatch;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public abstract class AbstractPlainJavaFxTest extends ApplicationTest {

  private final Pane root;
  private Scene scene;
  private Stage stage;
  private List<Throwable> exceptionsCaught = new ArrayList<>();

  public AbstractPlainJavaFxTest() {
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
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
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
    loader.setController(controller);
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
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
}
