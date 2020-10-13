package com.faforever.client.ui.alert.animation;

import com.jfoenix.transitions.CachedTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

public class CenterTransition extends CachedTransition {
  public CenterTransition(Node contentContainer, Node overlay) {
    super(contentContainer, new Timeline(
        new KeyFrame(Duration.ZERO,
            new KeyValue(contentContainer.scaleXProperty(), 0, Interpolator.LINEAR),
            new KeyValue(contentContainer.scaleYProperty(), 0, Interpolator.LINEAR),
            new KeyValue(overlay.opacityProperty(), 0, Interpolator.EASE_BOTH)
        ),
        new KeyFrame(Duration.millis(1000),
            new KeyValue(contentContainer.scaleXProperty(), 1, Interpolator.EASE_OUT),
            new KeyValue(contentContainer.scaleYProperty(), 1, Interpolator.EASE_OUT),
            new KeyValue(overlay.opacityProperty(), 1, Interpolator.EASE_BOTH)
        )));
    setCycleDuration(Duration.seconds(0.4));
    setDelay(Duration.seconds(0));
  }
}
