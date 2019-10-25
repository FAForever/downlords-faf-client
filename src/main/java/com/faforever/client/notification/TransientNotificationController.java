package com.faforever.client.notification;

import com.faforever.client.fx.Controller;
import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.preferences.PreferencesService;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static javafx.util.Duration.millis;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TransientNotificationController implements Controller<Node> {

  private final PreferencesService preferencesService;
  public Pane transientNotificationRoot;
  public Label messageLabel;
  public Label titleLabel;
  public ImageView imageView;
  private ChangeListener<Number> animationListener;
  private ActionCallback action;
  private Timeline timeline;
  private int toastDisplayTime;

  public void initialize() {
    Rectangle rectangle = new Rectangle();
    rectangle.widthProperty().bind(transientNotificationRoot.widthProperty());
    rectangle.heightProperty().bind(transientNotificationRoot.heightProperty());

    // Wait until the height is known, then animate it
    this.animationListener = (observable, oldValue, newValue) -> {
      if (newValue != null) {
        observable.removeListener(animationListener);
        transientNotificationRoot.setMaxHeight(0);
        transientNotificationRoot.setVisible(true);
        TransientNotificationController.this.animate(newValue);
      }
    };

    transientNotificationRoot.setVisible(false);
    transientNotificationRoot.setClip(rectangle);
    transientNotificationRoot.heightProperty().addListener(animationListener);

    // Divided by two because it's used in a cycle
    toastDisplayTime = preferencesService.getPreferences().getNotification().getToastDisplayTime() / 2;

    transientNotificationRoot.setOnMouseEntered(event -> timeline.pause());
    transientNotificationRoot.setOnMouseExited(event -> timeline.playFrom(Duration.millis(300 + toastDisplayTime)));
  }

  private void animate(Number height) {
    timeline = new Timeline();
    timeline.setAutoReverse(true);
    timeline.setCycleCount(2);
    timeline.getKeyFrames().addAll(
        new KeyFrame(millis(300), new KeyValue(transientNotificationRoot.maxHeightProperty(), height, Interpolator.LINEAR)),
        new KeyFrame(millis(300 + toastDisplayTime), new KeyValue(transientNotificationRoot.maxHeightProperty(), height))
    );
    timeline.setOnFinished(event -> dismiss());
    timeline.playFromStart();
  }

  private void dismiss() {
    Objects.requireNonNull(timeline, "timeline has not been set");
    timeline.stop();
    Pane parent = (Pane) transientNotificationRoot.getParent();
    if (parent == null) {
      return;
    }
    parent.getChildren().remove(transientNotificationRoot);
  }

  public void setNotification(TransientNotification notification) {
    titleLabel.setText(notification.getTitle());
    messageLabel.setText(notification.getText());
    imageView.setImage(notification.getImage());
    action = notification.getActionCallback();
  }

  public void onCloseButtonClicked() {
    dismiss();
  }

  public Region getRoot() {
    return transientNotificationRoot;
  }

  public void onClicked(MouseEvent event) {
    if (event.getButton().equals(MouseButton.SECONDARY)) {
      dismiss();
    } else if (action != null) {
      action.call(event);
    }
  }
}
