package com.faforever.client.notification;

import com.faforever.client.preferences.PreferencesService;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import javax.annotation.Resource;

import static javafx.util.Duration.millis;

public class TransientNotificationController {

  @FXML
  Pane transientNotificationRoot;
  @FXML
  Label messageLabel;
  @FXML
  Label titleLabel;
  @FXML
  ImageView imageView;

  @Resource
  PreferencesService preferencesService;

  private ChangeListener<Number> animationListener;

  @FXML
  void initialize() {
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
  }

  private void animate(Number height) {
    int toastDisplayTime = preferencesService.getPreferences().getNotification().getToastDisplayTime();

    Timeline timeline = new Timeline();
    timeline.setAutoReverse(true);
    timeline.setCycleCount(2);
    timeline.getKeyFrames().addAll(
        new KeyFrame(millis(300), new KeyValue(transientNotificationRoot.maxHeightProperty(), height, Interpolator.LINEAR)),
        new KeyFrame(millis(300 + toastDisplayTime), new KeyValue(transientNotificationRoot.maxHeightProperty(), height))
    );
    timeline.setOnFinished(event -> ((Pane) transientNotificationRoot.getParent()).getChildren().remove(transientNotificationRoot));
    timeline.playFromStart();
  }

  public void setNotification(TransientNotification notification) {
    titleLabel.setText(notification.getTitle());
    messageLabel.setText(notification.getText());
    imageView.setImage(notification.getImage());
  }

  @FXML
  void onCloseButtonClicked() {
    dismiss();
  }

  private void dismiss() {
    ((Pane) transientNotificationRoot.getParent()).getChildren().remove(this.getRoot());
  }

  public Region getRoot() {
    return transientNotificationRoot;
  }
}
