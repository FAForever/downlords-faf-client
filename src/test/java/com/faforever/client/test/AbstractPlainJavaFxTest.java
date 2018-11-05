package com.faforever.client.test;

import com.faforever.client.ui.StageHolder;
import com.github.nocatch.NoCatch.NoCatchRunnable;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.testfx.framework.junit.ApplicationTest;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static com.github.nocatch.NoCatch.noCatch;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractPlainJavaFxTest extends ApplicationTest {

  private final Pane root;
  private Scene scene;
  private Stage stage;

  public AbstractPlainJavaFxTest() {
    root = new Pane();
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

    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(getThemeFileUrl(fileName));
    loader.setResources(new MessageSourceResourceBundle(messageSource, Locale.US));
    loader.setControllerFactory(controllerFactory);
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      noCatch((Callable<Object>) loader::load);
      latch.countDown();
    });
    noCatch((NoCatchRunnable) latch::await);
  }

  protected String getThemeFile(String file) {
    return String.format("/%s", file);
  }

  protected URL getThemeFileUrl(String file) {
    return noCatch(() -> new ClassPathResource(getThemeFile(file)).getURL());
  }
}
