package com.faforever.client.ui.alert.animation;

import com.jfoenix.transitions.CachedTransition;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.function.Function;

public interface AlertAnimation {

  Function<Transition, Transition> inverseAnimation = transition -> {
    transition.jumpTo(transition.getCycleDuration());
    transition.setRate(-1);
    return transition;
  };

  void initAnimation(Node contentContainer, Node overlay);

  Animation createShowingAnimation(Node contentContainer, Node overlay);

  Animation createHidingAnimation(Node contentContainer, Node overlay);

  AlertAnimation LEFT_ANIMATION = new AlertAnimation() {
    @Override
    public void initAnimation(Node contentContainer, Node overlay) {
      overlay.setOpacity(0);
      contentContainer.setTranslateX(-(contentContainer.getLayoutX()
          + contentContainer.getLayoutBounds().getMaxX()));
    }

    @Override
    public Animation createShowingAnimation(Node contentContainer, Node overlay) {
      return new HorizontalTransition(true, contentContainer, overlay);
    }

    @Override
    public Animation createHidingAnimation(Node contentContainer, Node overlay) {
      return inverseAnimation.apply(new HorizontalTransition(true, contentContainer, overlay));
    }
  };

  AlertAnimation RIGHT_ANIMATION = new AlertAnimation() {
    @Override
    public void initAnimation(Node contentContainer, Node overlay) {
      overlay.setOpacity(0);
      contentContainer.setTranslateX(contentContainer.getLayoutX()
          + contentContainer.getLayoutBounds().getMaxX());
    }

    @Override
    public Animation createShowingAnimation(Node contentContainer, Node overlay) {
      return new HorizontalTransition(false, contentContainer, overlay);
    }

    @Override
    public Animation createHidingAnimation(Node contentContainer, Node overlay) {
      return inverseAnimation.apply(new HorizontalTransition(false, contentContainer, overlay));
    }
  };

  AlertAnimation TOP_ANIMATION = new AlertAnimation() {
    @Override
    public void initAnimation(Node contentContainer, Node overlay) {
      overlay.setOpacity(0);
      contentContainer.setTranslateY(-(contentContainer.getLayoutY()
          + contentContainer.getLayoutBounds().getMaxY()));
    }

    @Override
    public Animation createShowingAnimation(Node contentContainer, Node overlay) {
      return new VerticalTransition(true, contentContainer, overlay);
    }

    @Override
    public Animation createHidingAnimation(Node contentContainer, Node overlay) {
      return inverseAnimation.apply(new VerticalTransition(true, contentContainer, overlay));
    }
  };

  AlertAnimation BOTTOM_ANIMATION = new AlertAnimation() {
    @Override
    public void initAnimation(Node contentContainer, Node overlay) {
      overlay.setOpacity(0);
      contentContainer.setTranslateY(contentContainer.getLayoutY()
          + contentContainer.getLayoutBounds().getMaxY());
    }

    @Override
    public Animation createShowingAnimation(Node contentContainer, Node overlay) {
      return new VerticalTransition(false, contentContainer, overlay);
    }

    @Override
    public Animation createHidingAnimation(Node contentContainer, Node overlay) {
      return inverseAnimation.apply(new VerticalTransition(false, contentContainer, overlay));
    }
  };

  AlertAnimation CENTER_ANIMATION = new AlertAnimation() {
    @Override
    public void initAnimation(Node contentContainer, Node overlay) {
      overlay.setOpacity(0);
      contentContainer.setScaleX(0);
      contentContainer.setScaleY(0);
    }

    @Override
    public Animation createShowingAnimation(Node contentContainer, Node overlay) {
      return new CenterTransition(contentContainer, overlay);
    }

    @Override
    public Animation createHidingAnimation(Node contentContainer, Node overlay) {
      return inverseAnimation.apply(new CenterTransition(contentContainer, overlay));
    }
  };

  AlertAnimation NO_ANIMATION = new AlertAnimation() {
    @Override
    public void initAnimation(Node contentContainer, Node overlay) {

    }

    @Override
    public Animation createShowingAnimation(Node contentContainer, Node overlay) {
      return null;
    }

    @Override
    public Animation createHidingAnimation(Node contentContainer, Node overlay) {
      return null;
    }
  };

  AlertAnimation SMOOTH = new AlertAnimation() {
    @Override
    public void initAnimation(Node contentContainer, Node overlay) {
      overlay.setOpacity(0);
      contentContainer.setScaleX(.80);
      contentContainer.setScaleY(.80);
    }

    @Override
    public Animation createShowingAnimation(Node contentContainer, Node overlay) {
      return new CachedTransition(contentContainer, new Timeline(
          new KeyFrame(Duration.millis(1000),
              new KeyValue(contentContainer.scaleXProperty(), 1, Interpolator.EASE_OUT),
              new KeyValue(contentContainer.scaleYProperty(), 1, Interpolator.EASE_OUT),
              new KeyValue(overlay.opacityProperty(), 1, Interpolator.EASE_BOTH)
          ))) {
        {
          setCycleDuration(Duration.millis(160));
          setDelay(Duration.seconds(0));
        }
      };
    }

    @Override
    public Animation createHidingAnimation(Node contentContainer, Node overlay) {
      return new CachedTransition(contentContainer, new Timeline(
          new KeyFrame(Duration.millis(1000),
              new KeyValue(overlay.opacityProperty(), 0, Interpolator.EASE_BOTH)
          ))) {
        {
          setCycleDuration(Duration.millis(160));
          setDelay(Duration.seconds(0));
        }
      };
    }
  };
}

