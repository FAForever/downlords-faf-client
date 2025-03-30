package com.faforever.client.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.function.Consumer;

public class MouseEventUtil {
  private static Timeline singleClickTimeline;

  public static void handleClick(MouseEvent event, Runnable doubleClickAction, Consumer<MouseEvent> singleClickAction) {
    if (event.getClickCount() == 2) {
      if (singleClickTimeline != null) {
        singleClickTimeline.stop();
        singleClickTimeline = null;
      }
      doubleClickAction.run();
      event.consume();
    } else if (event.getClickCount() == 1) {
      if (singleClickTimeline != null) {
        singleClickTimeline.stop();
      }
      singleClickTimeline = new Timeline(new KeyFrame(Duration.millis(200), ae -> {
        singleClickTimeline = null;
        singleClickAction.accept(event);
      }));
      singleClickTimeline.setCycleCount(1);
      singleClickTimeline.play();
    }
  }
}