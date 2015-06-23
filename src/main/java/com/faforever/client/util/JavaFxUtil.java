package com.faforever.client.util;

import com.sun.webkit.WebPage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Tooltip;
import javafx.scene.web.WebEngine;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Utility class to fix some annoying JavaFX shortcomings.
 */
public class JavaFxUtil {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private JavaFxUtil() {
    // Utility class
  }

  /**
   * Creates a listener that waits for the document property of a WebEngine to change (a sign that the WebEngine has
   * loaded a new document) and sets the background color of the new web page to transparent. This is a workaround for
   * the JavaFX bug that's preventing WebView elements from having a transparent background by default.
   */
  public static void makeWebViewTransparent(WebEngine webEngine) {
    webEngine.documentProperty().addListener((arg0, arg1, arg2) -> {
      try {
        Field field = webEngine.getClass().getDeclaredField("page");
        field.setAccessible(true);

        WebPage page = (WebPage) field.get(webEngine);
        page.setBackgroundColor((new java.awt.Color(0, 0, 0, 0)).getRGB());
      } catch (Exception e) {
        logger.error("Failed to set the WebView's background to transparent", e);
      }
    });
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
