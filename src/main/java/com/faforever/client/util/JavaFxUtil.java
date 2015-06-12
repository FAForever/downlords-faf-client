package com.faforever.client.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Tooltip;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.lang.reflect.Field;

/**
 * Utility class to fix some annoying JavaFX shortcomings.
 */
public class JavaFxUtil {

  private JavaFxUtil() {
    // Utility class
  }

  /**
   * Uses reflection to change to tooltip delay/duration to some sane values.
   * <p>
   * See <a href="https://javafx-jira.kenai.com/browse/RT-19538">https://javafx-jira.kenai.com/browse/RT-19538</a>
   */
  public static void fixTooltipDuration() {
    try {
      Field fieldBehavior = Tooltip.class.getDeclaredField("BEHAVIOR");
      fieldBehavior.setAccessible(true);
      Object objBehavior = fieldBehavior.get(null);

      Field activationTimerField = objBehavior.getClass().getDeclaredField("activationTimer");
      activationTimerField.setAccessible(true);
      Timeline objTimer = (Timeline) activationTimerField.get(objBehavior);

      objTimer.getKeyFrames().setAll(new KeyFrame(new Duration(500)));

      Field hideTimerField = objBehavior.getClass().getDeclaredField("hideTimer");
      hideTimerField.setAccessible(true);
      objTimer = (Timeline) hideTimerField.get(objBehavior);

      objTimer.getKeyFrames().setAll(new KeyFrame(new Duration(100000)));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Centers a window FOR REAL. https://javafx-jira.kenai.com/browse/RT-40368
   */
  public static void centerOnScreen(Stage stage) {
    double width = stage.getWidth();
    double height = stage.getHeight();

    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    stage.setX((screenBounds.getWidth() - width) / 2);
    stage.setY((screenBounds.getHeight() - height) / 2);
  }

  public static void assertApplicationThread() {
    if (!Platform.isFxApplicationThread()) {
      throw new IllegalStateException("Must run in FX Application thread");
    }
  }

  public static void assertBackgroundThread() {
    if (Platform.isFxApplicationThread()) {
      throw new IllegalStateException("Must not run in FX Application thread");
    }
  }
}
