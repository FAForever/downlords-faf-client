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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javax.annotation.PostConstruct;
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
  private Action action;
  private Timeline timeline;
  private int toastDisplayTime;

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
    timeline.stop();
    Pane parent = (Pane) transientNotificationRoot.getParent();
    if (parent == null) {
      return;
    }
    parent.getChildren().remove(transientNotificationRoot);
  }

  @PostConstruct
  void postConstruct() {
    // Divided by two because it's used in a cycle
    toastDisplayTime = preferencesService.getPreferences().getNotification().getToastDisplayTime() / 2;

    transientNotificationRoot.setOnMouseEntered(event -> timeline.pause());
    transientNotificationRoot.setOnMouseExited(event -> timeline.playFrom(Duration.millis(300 + toastDisplayTime)));
  }

  public void setNotification(TransientNotification notification) {
    titleLabel.setText(notification.getTitle());
    messageLabel.setText(notification.getText());
    imageView.setImage(notification.getImage());
    action = notification.getAction();
  }

  @FXML
  void onCloseButtonClicked() {
    dismiss();
  }

  public Region getRoot() {
    return transientNotificationRoot;
  }

  @FXML
  public void onClicked(MouseEvent event) {
    action.call(event);
  }
}
