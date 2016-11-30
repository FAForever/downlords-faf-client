package com.faforever.client.test;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static com.github.nocatch.NoCatch.noCatch;

public class AbstractPlainJavaFxTest extends ApplicationTest {

  private final Pane root;
  @Mock
  ResourceBundle resources;
  private Scene scene;
  private Stage stage;

  public AbstractPlainJavaFxTest() {
    root = new Pane();
  }

  @Override
  public void start(Stage stage) throws Exception {
    this.stage = stage;
    MockitoAnnotations.initMocks(this);

    scene = new Scene(getRoot(), 1, 1);
    stage.setScene(scene);
    stage.show();
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

  protected <T> T loadController(String fileName) throws IOException {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.Messages");

    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(getClass().getResource(getThemeFile(fileName)));
    loader.setResources(new MessageSourceResourceBundle(messageSource, Locale.US));
    WaitForAsyncUtils.waitForAsyncFx(5000, (Callable<Object>) loader::load);
    return loader.getController();
  }

  protected String getThemeFile(String file) {
    return String.format("/theme/%s", file);
  }

  protected URL getThemeFileUrl(String file) {
    return noCatch(() -> new ClassPathResource(getThemeFile(file)).getURL());
  }
}
