package com.faforever.client.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
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
    double width = stage.getWidth();
    double height = stage.getHeight();

    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    stage.setX((screenBounds.getWidth() - width) / 2);
    stage.setY((screenBounds.getHeight() - height) / 2);
  }
}
