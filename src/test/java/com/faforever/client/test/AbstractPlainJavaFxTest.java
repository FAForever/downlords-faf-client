package com.faforever.client.test;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.Assert;
import org.testfx.framework.junit.ApplicationTest;

public class AbstractPlainJavaFxTest extends ApplicationTest {

  @Override
  public void start(Stage stage) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(this::uncaughtException);

    Scene scene = new Scene(new Pane(), 1, 1);
    stage.setScene(scene);
    stage.show();

  }

  private void uncaughtException(Thread t, Throwable e) {
    e.printStackTrace();
    Assert.fail(e.getMessage());
  }
}
