package com.faforever.client.test;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit.ApplicationTest;

import java.lang.invoke.MethodHandles;

public class AbstractPlainJavaFxTest extends ApplicationTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Pane root;
  private Scene scene;
  private Stage stage;

  public AbstractPlainJavaFxTest() {
    root = new Pane();
  }

  @Override
  public void start(Stage stage) throws Exception {
    this.stage = stage;
    Thread.setDefaultUncaughtExceptionHandler(this::uncaughtException);

    scene = new Scene(getRoot(), 1, 1);
    stage.setScene(scene);
    stage.show();
  }

  protected Pane getRoot() {
    return root;
  }

  private void uncaughtException(Thread t, Throwable e) {
    logger.warn("Uncaught exception", e);
    Assert.fail(e.getMessage());
  }

  protected Scene getScene() {
    return scene;
  }

  protected Stage getStage() {
    return stage;
  }
}
