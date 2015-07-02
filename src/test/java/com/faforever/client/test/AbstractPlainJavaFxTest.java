package com.faforever.client.test;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.Assert;
import org.testfx.framework.junit.ApplicationTest;

public class AbstractPlainJavaFxTest extends ApplicationTest {

  private Scene scene;
  private Pane root;

  public AbstractPlainJavaFxTest() {
    root = new Pane();
  }

  @Override
  public void start(Stage stage) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(this::uncaughtException);

    scene = new Scene(getRoot(), 1, 1);
    stage.setScene(scene);
    stage.show();
  }

  protected Pane getRoot() {
    return root;
  }

  private void uncaughtException(Thread t, Throwable e) {
    e.printStackTrace();
    Assert.fail(e.getMessage());
  }

  protected Scene getScene() {
    return scene;
  }
}
