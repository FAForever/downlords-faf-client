package com.faforever.client.test;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class AbstractPlainJavaFxTest extends ApplicationTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

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
    Thread.setDefaultUncaughtExceptionHandler(this::uncaughtException);

    scene = new Scene(getRoot(), 1, 1);
    stage.setScene(scene);
    stage.show();
  }

  protected Pane getRoot() {
    return root;
  }

  private void uncaughtException(Thread t, Throwable e) {
    logger.warn("Uncaught exception", e.getCause());
    Assert.fail(e.getMessage());
  }

  protected <T> T loadController(String fileName) throws Exception {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.Messages");

    FXMLLoader loader = new FXMLLoader();
    loader.setResources(new MessageSourceResourceBundle(messageSource, Locale.US));

    return WaitForAsyncUtils.asyncFx(new Callable<T>() {
      @Override
      public T call() throws Exception {
        loader.load(getClass().getResourceAsStream("/themes/default/" + fileName));
        return loader.getController();
      }
    }).get(TIMEOUT, TIMEOUT_UNIT);
  }

  protected Scene getScene() {
    return scene;
  }

  protected Stage getStage() {
    return stage;
  }
}
