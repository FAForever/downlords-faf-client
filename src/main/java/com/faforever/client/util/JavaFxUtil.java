package com.faforever.client.util;

import javafx.stage.Stage;

public class JavaFxUtil {

  private JavaFxUtil() {
    // Utility class
  }

  /**
   * Centers a Window FOR REAL. https://javafx-jira.kenai.com/browse/RT-40368
   *
   * @param stage
   */
  public static void centerOnScreen(Stage stage) {
    stage.centerOnScreen();
    stage.setY(stage.getY() * 3f / 2f);
  }
}
